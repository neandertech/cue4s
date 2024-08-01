/*
 * Copyright 2023 Anton Sviridov
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
