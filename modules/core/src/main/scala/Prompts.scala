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

class Prompts private (
    protected val out: Output,
    protected val terminal: Terminal,
    protected val theme: Theme,
) extends AutoCloseable
    with PromptsPlatform:

  protected lazy val inputProvider = InputProvider(terminal)

  override def close(): Unit =
    terminal.cursorShow()
    inputProvider.close()
end Prompts

object Prompts extends PromptsCompanionPlatform:

  @deprecated(
    "Use `Prompts.sync.use(...)` or `Prompts.async.use(...)` instead, this method will be removed in 0.1.0",
    "0.0.4",
  )
  def apply(
      out: Output = Output.Std,
      createTerminal: Output => Terminal = Terminal.ansi,
      theme: Theme = Theme.Default,
  ) = new Prompts(out, createTerminal(out), theme)

  @deprecated(
    "Use `Prompts.sync.use(...)` or `Prompts.async.use(...)` instead, this method will be removed in 0.1.0",
    "0.0.4",
  )
  def use[A](
      out: Output = Output.Std,
      createTerminal: Output => Terminal = Terminal.ansi,
      theme: Theme = Theme.Default,
  )(f: Prompts => A): A =
    val prompts = apply(out, createTerminal, theme)
    try f(prompts)
    finally prompts.close()
  end use

end Prompts
