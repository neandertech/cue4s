package proompts.catseffect

import proompts.*
import cats.effect.*

class PromptsIO private (
    protected val out: Output,
    protected val terminal: Terminal,
    protected val colors: Boolean
) extends AutoCloseable:
  protected lazy val inputProvider = InputProvider(out)

  def io[A](
      prompt: Prompt[A],
      out: Output = Output.Std,
      createTerminal: Output => Terminal = Terminal.ansi(_),
      colors: Boolean = true
  ): IO[Completion[A]] =
    val terminal      = createTerminal(out)
    val inputProvider = InputProvider(out)
    val handler       = prompt.handler(terminal, out, colors)

    // TODO: provide native CE interface here
    IO.executionContext.flatMap: ec =>
      IO.fromFuture(IO(inputProvider.evaluateFuture(handler)(using ec)))
  end io

  override def close(): Unit = inputProvider.close()
end PromptsIO

object PromptsIO:
  def apply(
      out: Output = Output.Std,
      createTerminal: Output => Terminal = Terminal.ansi,
      colors: Boolean = true
  ): Resource[IO, PromptsIO] = Resource.fromAutoCloseable(
    IO(new PromptsIO(out, createTerminal(out), colors))
  )
