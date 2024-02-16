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

def errln(o: Any) = System.err.println(o)

// class Interactive[Result](
//     terminal: Terminal,
//     prompt: Prompt[Result],
//     out: Output,
//     colors: Boolean
// ):
//   val handler: Handler[Result] =
//     prompt match
//       case p: Prompt.Input =>
//         // TODO - reorg the codebase so this instanceOf is not required
//         InteractiveTextInput(p, terminal, out, colors).handler.asInstanceOf
//       case p: Prompt.Alternatives =>
//         // TODO - reorg the codebase so this instanceOf is not required
//         InteractiveAlternatives(terminal, p, out, colors).handler.asInstanceOf

// end Interactive
