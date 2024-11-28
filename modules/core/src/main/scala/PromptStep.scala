/*
 * Copyright 2023 Neandertech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cue4s

private[cue4s] case class PromptStep[S, T](
    prompt: Prompt[T],
    set: (S, T) => S,
):
  def run(
      exec: Prompt[T] => Completion[T],
      s: S,
      log: String => Unit,
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
      set = (s, a) => t.set(s, a.asInstanceOf[T]),
    ):
      override def run(
          exec: Prompt[Any] => Completion[Any],
          s: S,
          log: String => Unit,
      ): S | CompletionError =
        t.run(exec.asInstanceOf, s, log)
    end new
  end toAny
end PromptStep
