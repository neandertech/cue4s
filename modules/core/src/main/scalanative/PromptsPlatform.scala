package proompts

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

transparent trait PromptsPlatform:
  self: Prompts =>

  def sync[R](
      prompt: Prompt[R],
      out: Output = Output.Std,
      createTerminal: Output => Terminal = Terminal.ansi(_),
      colors: Boolean = true
  ): Completion[R] =
    val handler = prompt.handler(terminal, out, colors)

    inputProvider.evaluate(handler)
  end sync

  def future[R](
      prompt: Prompt[R],
      out: Output = Output.Std,
      createTerminal: Output => Terminal = Terminal.ansi(_),
      colors: Boolean = true
  )(using ExecutionContext): Future[Completion[R]] =
    val handler = prompt.handler(createTerminal(out), out, colors)

    inputProvider.evaluateFuture(handler)
  end future
end PromptsPlatform
