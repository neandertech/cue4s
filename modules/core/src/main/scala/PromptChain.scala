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

package proompts

private[proompts] object PromptChain:
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
