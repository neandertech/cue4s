package cue4s

private[cue4s] case class PromptStep[S, T](
    prompt: Prompt[T],
    set: (S, T) => S
):
  def run(
      exec: Prompt[T] => Completion[T],
      s: S,
      log: String => Unit
  ): S | CompletionError =
    exec(prompt) match
      case Completion.Finished(value) => set(s, value)
      case Completion.Fail(value)     => value
      // case Completion.Interrupted     => Completion.Interrupted
      // case err @ Completion.Error(_)  => err
  end run

  def toAny: PromptStep[S, Any] =
    val t = this
    new PromptStep[S, Any](
      prompt.asInstanceOf[Prompt[Any]],
      set = (s, a) => t.set(s, a.asInstanceOf[T])
    ):
      override def run(
          exec: Prompt[Any] => Completion[Any],
          s: S,
          log: String => Unit
      ): S | CompletionError =
        t.run(exec.asInstanceOf, s, log)
    end new
  end toAny
end PromptStep
