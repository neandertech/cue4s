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

package com.indoorvivants.proompts

def errln(o: Any) = System.err.println(o)

class Interactive(
    terminal: Terminal,
    prompt: Prompt,
    out: Output,
    colors: Boolean
):
  val handler =
    prompt match
      case p: Prompt.Input => InteractiveTextInput(p, terminal, out, colors).handler
      case p: Prompt.Alternatives =>
        InteractiveAlternatives(terminal, p, out, colors).handler

end Interactive
