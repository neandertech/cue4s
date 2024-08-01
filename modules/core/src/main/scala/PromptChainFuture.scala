/*
 * Copyright 2023 Anton Sviridov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package proompts

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/** This functionality is not stable yet and therefore marked private
  *
  * @param init
  * @param terminal
  * @param out
  * @param colors
  * @param reversedSteps
  */
private[proompts] case class PromptChainFuture[A] private[proompts] (
    init: A,
    terminal: Terminal,
    out: Output,
    colors: Boolean,
    reversedSteps: List[
      ExecutionContext ?=> A => Future[A]
    ]
):
  def prompt[R](
      nextPrompt: A => Prompt[R] | Future[Prompt[R]],
      updateValue: (A, R) => A | Future[A]
  ) =
    val step = (ec: ExecutionContext) ?=>
      (a: A) =>
        lift(nextPrompt)(a).flatMap: prompt =>
          eval(prompt): nextResult =>
            lift(updateValue.tupled)(a, nextResult)

    copy(reversedSteps = step :: reversedSteps)
  end prompt

  def evaluateFuture(using ExecutionContext): Future[A] =
    reversedSteps.reverse.foldLeft(Future.successful(init)):
      case (acc, step) =>
        acc.flatMap(step)
  end evaluateFuture

  private def fail(msg: String) = Future.failed(new RuntimeException(msg))
  private def check[T, R](c: Future[Completion[R]])(v: R => Future[T])(using
      ExecutionContext
  ): Future[T] =
    c.flatMap(check(_)(v))

  private def lift[A, B](f: A => B | Future[B]): A => Future[B] =
    a =>
      f(a) match
        case f: Future[?] => f.asInstanceOf[Future[B]]
        case other        => Future.successful(other.asInstanceOf[B])

  private def eval[T, R](p: Prompt[R])(v: R => Future[T])(using
      ExecutionContext
  ): Future[T] = check(
    inputProvider.evaluateFuture(handler(p))
  )(v)

  private def check[T, R](c: Completion[R])(v: R => Future[T]): Future[T] =
    c match
      case Completion.Interrupted =>
        fail("interrupted")
      case Completion.Error(msg) =>
        fail(msg)
      case Completion.Finished(value) =>
        v(value)

  private def inputProvider = InputProvider(out)
  private def handler[R](prompt: Prompt[R]) =
    prompt.handler(terminal, out, colors)
end PromptChainFuture
