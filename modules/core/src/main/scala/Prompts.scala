package proompts

class Prompts private (
    protected val out: Output,
    protected val terminal: Terminal,
    protected val colors: Boolean
) extends AutoCloseable
    with PromptsPlatform:

  protected lazy val inputProvider = InputProvider(out)

  override def close(): Unit = inputProvider.close()
end Prompts

object Prompts:
  def apply(
      out: Output = Output.Std,
      createTerminal: Output => Terminal = Terminal.ansi,
      colors: Boolean = true
  ) = new Prompts(out, createTerminal(out), colors)
