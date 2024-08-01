package proompts.catseffect

import cats.effect.*
import proompts.*

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

// extension (p: PromptChain.type)
//   private def io[A](
//       init: A,
//       terminal: Terminal = Terminal.ansi(Output.Std),
//       out: Output = Output.Std,
//       colors: Boolean = true
//   ): PromptChainIO[A] =
//     new PromptChainIO[A](
//       init = init,
//       terminal = terminal,
//       out = out,
//       colors = colors,
//       reversedSteps = Nil
//     )

extension (p: RunPrompt.type)
  def io[A](
      prompt: Prompt[A],
      out: Output = Output.Std,
      createTerminal: Output => Terminal = Terminal.ansi(_),
      colors: Boolean = true
  ): IO[Completion[A]] =
    val terminal      = createTerminal(out)
    val inputProvider = InputProvider(out)
    val handler       = prompt.handler(terminal, out, colors)

    IO.executionContext.flatMap: ec =>
      IO.fromFuture(IO(inputProvider.evaluateFuture(handler)(using ec)))
