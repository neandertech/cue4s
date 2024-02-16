package proompts

object PromptChain:
  def future[A](
      init: A,
      terminal: Terminal = Terminal.ansi(Output.Std),
      out: Output = Output.Std,
      colors: Boolean = true
  ): PromptChainFuture[A] =
    new PromptChainFuture[A](
      init = init,
      terminal = terminal,
      out = out,
      colors = colors,
      reversedSteps = Nil
    )
end PromptChain
