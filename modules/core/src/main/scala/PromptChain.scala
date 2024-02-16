package com.indoorvivants.proompts

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

type OrError[A] = String | A

case class PromptChainFuture[A](
    terminal: Terminal,
    out: Output,
    colors: Boolean,
    start: (Prompt, String => A | Future[A]),
    reversedChain: List[
      (String => Prompt | Future[Prompt], (A, String) => A | Future[A])
    ]
):
  def evaluateFuture(using ExecutionContext): Future[A] =
    val (startPrompt, startTransform) = start
    val chain                         = reversedChain.reverse

    eval(startPrompt): startResult =>
      val init = lift(startTransform)(startResult)

      chain.foldLeft(init):
        case (acc, (nextPrompt, nextValueTransform)) =>
          acc.flatMap: a =>
            lift(nextPrompt)(startResult).flatMap: prompt =>
              eval(prompt): nextResult =>
                lift(nextValueTransform.tupled)(a, nextResult)

  end evaluateFuture

  def andThen(
      nextPrompt: String => Prompt | Future[Prompt],
      updateValue: (A, String) => A | Future[A]
  ) =
    copy(reversedChain = (nextPrompt, updateValue) :: reversedChain)

  private def fail(msg: String) = Future.failed(new RuntimeException(msg))
  private def check[T](c: Future[Completion])(v: String => Future[T])(using
      ExecutionContext
  ): Future[T] =
    c.flatMap(check(_)(v))

  private def lift[A, B](f: A => B | Future[B]): A => Future[B] =
    a =>
      f(a) match
        case f: Future[?] => f.asInstanceOf[Future[B]]
        case other        => Future.successful(other.asInstanceOf[B])

  private def eval[T](p: Prompt)(v: String => Future[T])(using
      ExecutionContext
  ): Future[T] = check(
    ip.evaluateFuture(interactive(p))
  )(v)

  private def check[T](c: Completion)(v: String => Future[T]): Future[T] =
    c match
      case Completion.Interrupted =>
        fail("interrupted")
      case Completion.Error(msg) =>
        fail(msg)
      case Completion.Finished(value) =>
        v(value)

  private def ip = InputProvider(out)
  private def interactive(prompt: Prompt) =
    Interactive(terminal, prompt, out, colors)
end PromptChainFuture

object PromptChain:
  def future[A](
      start: Prompt,
      createValue: String => A | Future[A],
      terminal: Terminal,
      out: Output,
      colors: Boolean
  ) =
    new PromptChainFuture[A](
      terminal,
      out,
      colors,
      start = (start, createValue),
      reversedChain = Nil
    )
end PromptChain
