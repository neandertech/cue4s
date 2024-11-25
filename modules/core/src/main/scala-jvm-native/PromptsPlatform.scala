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
import scala.reflect.Typeable

private trait PromptsPlatform:
  self: Prompts =>

  def sync[R: Typeable](
      prompt: Prompt[R] | PromptChain[R]
  ): Completion[R] =
    prompt match
      case p: Prompt[R] =>
        try
          val handler = p.handler(terminal, out, colors)

          inputProvider.evaluate(handler)
        finally
          terminal.cursorShow()
          inputProvider.close()
      // ensure prompt doesn't forget cleaning up after itself

      case c: PromptChain[R] =>
        c.run([t] => (p: Prompt[t]) => sync(p))
  end sync

  def future[R](
      prompt: Prompt[R],
      out: Output = Output.Std,
      createTerminal: Output => Terminal = Terminal.ansi(_),
      colors: Boolean = true
  )(using ExecutionContext): Future[Completion[R]] =
    val handler = prompt.handler(createTerminal(out), out, colors)

    val f = inputProvider.evaluateFuture(handler)
    f.onComplete(_ => terminal.cursorShow())
    f.onComplete(_ => inputProvider.close())
    f
  end future
end PromptsPlatform
