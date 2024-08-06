package cue4s

import scala.util.boundary

private[cue4s] case class PromptPlan[Into](
    prompts: List[PromptStep[Tuple, Any]],
    finish: Tuple => Into
):

  def withSteps(step: Seq[PromptStep[Tuple, Any]]) =
    copy(prompts = step.toList ++ prompts)

  def run(exec: [t] => Prompt[t] => Completion[t]): Completion[Into] =
    var raw: Tuple = EmptyTuple
    boundary[Completion[Into]]:
      prompts.foreach: step =>
        step.run(exec[Any], raw, System.err.println(_)) match
          case t: Tuple              => raw = t
          case what: CompletionError => boundary.break(Completion.Fail(what))
      Completion.Finished(finish(raw))
  end run
end PromptPlan
