package proompts

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

object RunPrompt:
  def future[R](
      prompt: Prompt[R],
      out: Output = Output.Std,
      createTerminal: Output => Terminal = Terminal.ansi(_),
      colors: Boolean = true
  )(using ExecutionContext): Future[Completion[R]] =
    val handler = prompt.handler(createTerminal(out), out, colors)
    val inputProvider = InputProvider(out)

    inputProvider.evaluateFuture(handler)
