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

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

private trait PromptsPlatform:
  self: Prompts =>

  def run[R](
      prompt: Prompt[R] | PromptChain[R],
  ): Completion[R] =
    prompt match
      case p: Prompt[?] =>
        val framework =
          p.asInstanceOf[Prompt[R]].framework(terminal, out, theme)

        inputProvider.evaluate(framework.handler)

      case c: PromptChain[?] =>
        c.asInstanceOf[PromptChain[R]].run([t] => (p: Prompt[t]) => sync(p))
  end run

  def runAsync[R](
      prompt: Prompt[R],
  )(using ExecutionContext): Future[Completion[R]] =
    val framework = prompt.framework(terminal, out, theme)

    inputProvider.evaluateFuture(framework.handler)
  end runAsync

  @deprecated(
    "Use `run(...)` instead, this method will be removed in 0.1.0",
    "0.0.5",
  )
  def sync[R](
      prompt: Prompt[R] | PromptChain[R],
  ): Completion[R] = run(prompt)
  end sync

  @deprecated(
    "Use `runAsync(...)` instead, this method will be removed in 0.1.0",
    "0.0.5",
  )
  def future[R](
      prompt: Prompt[R],
  )(using ExecutionContext): Future[Completion[R]] = runAsync(prompt)
  end future
end PromptsPlatform
