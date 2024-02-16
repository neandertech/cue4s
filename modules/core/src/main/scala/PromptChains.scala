package com.indoorvivants.proompts

import scala.concurrent.Future

object PromptChains:
  def future[A](
      startPrompt: Prompt,
      createValue: String => A | Future[A]
  ): PromptChainFuture[A] =
    val out      = Output.Std
    val terminal = Terminal.ansi(out)
    val colors   = true
    PromptChain.future(
      start = startPrompt,
      createValue = createValue,
      terminal = terminal,
      out = out,
      colors = colors
    )
  end future
end PromptChains
