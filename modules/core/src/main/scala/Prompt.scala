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

trait Prompt[Result]:
  private[cue4s] def handler(
      terminal: Terminal,
      output: Output,
      colors: Boolean
  ): Handler[Result]

object Prompt:
  case class Input(
      lab: String,
      validate: String => Option[PromptError] = _ => None
  ) extends Prompt[String]:
    override def handler(
        terminal: Terminal,
        output: Output,
        colors: Boolean
    ): Handler[String] =
      InteractiveTextInput(this, terminal, output, colors).handler
  end Input

  case class SingleChoice(lab: String, alts: List[String])
      extends Prompt[String]:
    override def handler(
        terminal: Terminal,
        output: Output,
        colors: Boolean
    ): Handler[String] =
      InteractiveSingleChoice(this, terminal, output, colors).handler

  case class MultipleChoice(lab: String, alts: List[String])
      extends Prompt[List[String]]:
    override def handler(
        terminal: Terminal,
        output: Output,
        colors: Boolean
    ): Handler[List[String]] =
      InteractiveMultipleChoice(this, terminal, output, colors).handler
end Prompt
