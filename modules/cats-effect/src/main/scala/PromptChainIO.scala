/*
 * Copyright 2023 Neandertech
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

package cue4s.catseffect

import cats.effect.*
import cue4s.*

/** This functionality is not stable yet and therefore marked private
  *
  * @param init
  * @param terminal
  * @param out
  * @param colors
  * @param reversedSteps
  */
private[catseffect] case class PromptChainIO[A] private[catseffect] (
    init: A,
    terminal: Terminal,
    out: Output,
    colors: Boolean,
    reversedSteps: List[
      A => IO[A]
    ]
):
  def prompt[R](
      nextPrompt: A => Prompt[R] | IO[Prompt[R]],
      updateValue: (A, R) => A | IO[A]
  ) =
    val step =
      (a: A) =>
        lift(nextPrompt)(a).flatMap: prompt =>
          eval(prompt): nextResult =>
            lift(updateValue.tupled)(a, nextResult)

    copy(reversedSteps = step :: reversedSteps)
  end prompt

  def evaluateIO: IO[A] =
    reversedSteps.reverse.foldLeft(IO.pure(init)):
      case (acc, step) =>
        acc.flatMap(step)
  end evaluateIO

  private def lift[A, B](f: A => B | IO[B]): A => IO[B] =
    a =>
      f(a) match
        case f: IO[?] => f.asInstanceOf[IO[B]]
        case other    => IO.pure(other.asInstanceOf[B])

  private def eval[T, R](p: Prompt[R])(v: R => IO[T]): IO[T] =
    IO.executionContext.flatMap: ec =>
      IO.fromFuture(IO(inputProvider.evaluateFuture(handler(p))(using ec)))
        .flatMap(c => check(c)(v))

  private def check[T, R](c: Completion[R])(v: R => IO[T]): IO[T] =
    c match
      case Completion.Interrupted =>
        fail("interrupted")
      case Completion.Error(msg) =>
        fail(msg)
      case Completion.Finished(value) =>
        v(value)

  private lazy val inputProvider = InputProvider(out)
  private def handler[R](prompt: Prompt[R]) =
    prompt.handler(terminal, out, colors)

  private def fail(msg: String) = IO.raiseError(new RuntimeException(msg))

end PromptChainIO
