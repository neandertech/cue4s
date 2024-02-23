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

trait Prompt[Result]:
  def handler(
      terminal: Terminal,
      output: Output,
      colors: Boolean
  ): Handler[Result]

case class InputPrompt(lab: String) extends Prompt[String]:
  override def handler(
      terminal: Terminal,
      output: Output,
      colors: Boolean
  ): Handler[String] =
    InteractiveTextInput(this, terminal, output, colors).handler
end InputPrompt

case class AlternativesPrompt(lab: String, alts: List[String])
    extends Prompt[String]:
  override def handler(
      terminal: Terminal,
      output: Output,
      colors: Boolean
  ): Handler[String] =
    InteractiveAlternatives(this, terminal, output, colors).handler
end AlternativesPrompt

