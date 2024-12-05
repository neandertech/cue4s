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

import cue4s.Prompt.PasswordInput.Password

class SyncPromptsBuilder private (impl: SyncPromptsOptions):
  def this() = this(SyncPromptsOptions(Output.Std, Theme.Default))

  def noColors: SyncPromptsBuilder                = withTheme(Theme.NoColors)
  def withOutput(out: Output): SyncPromptsBuilder = copy(_.copy(out = out))
  def withTheme(theme: Theme): SyncPromptsBuilder = copy(_.copy(theme = theme))

  def use[A](f: SyncPrompts => A): A =
    val p    = Prompts(impl.out, Terminal.ansi, impl.theme)
    val inst = SyncPrompts(p)
    try
      f(inst)
    finally p.close()

  private def copy(f: SyncPromptsOptions => SyncPromptsOptions) =
    new SyncPromptsBuilder(f(impl))
end SyncPromptsBuilder

private case class SyncPromptsOptions(out: Output, theme: Theme)

class SyncPrompts(underlying: Prompts):
  export underlying.run

  def text(
      label: String,
      modify: Prompt.Input => Prompt.Input = identity,
  ): Completion[String] =
    run(modify(Prompt.Input(label)))

  def password(
      label: String,
      modify: Prompt.PasswordInput => Prompt.PasswordInput = identity,
  ): Completion[Password] =
    run(modify(Prompt.PasswordInput(label)))

  def int(
      label: String,
      modify: Prompt.NumberInput[Int] => Prompt.NumberInput[Int] = identity,
  ): Completion[Int] =
    run(modify(Prompt.NumberInput(label)))

  def float(
      label: String,
      modify: Prompt.NumberInput[Float] => Prompt.NumberInput[Float] = identity,
  ): Completion[Float] =
    run(modify(Prompt.NumberInput(label)))

  def double(
      label: String,
      modify: Prompt.NumberInput[Double] => Prompt.NumberInput[Double] =
        identity,
  ): Completion[Double] =
    run(modify(Prompt.NumberInput(label)))

  def singleChoice(
      label: String,
      options: List[String],
      modify: Prompt.SingleChoice => Prompt.SingleChoice = identity,
  ): Completion[String] =
    run(modify(Prompt.SingleChoice(label, options)))

  def confirm(
      label: String,
      modify: Prompt.Confirmation => Prompt.Confirmation = identity,
      default: Boolean = true,
  ): Completion[Boolean] =
    run(modify(Prompt.Confirmation(label, default)))

  def multiChoiceAllSelected(
      label: String,
      options: List[String],
      modify: Prompt.MultipleChoice => Prompt.MultipleChoice = identity,
  ): Completion[List[String]] =
    run(modify(Prompt.MultipleChoice.withAllSelected(label, options)))

  def multiChoiceNoneSelected(
      label: String,
      options: List[String],
      modify: Prompt.MultipleChoice => Prompt.MultipleChoice = identity,
  ): Completion[List[String]] =
    run(modify(Prompt.MultipleChoice.withNoneSelected(label, options)))

  def multiChoiceSomeSelected(
      label: String,
      options: List[(String, Boolean)],
      modify: Prompt.MultipleChoice => Prompt.MultipleChoice = identity,
  ): Completion[List[String]] =
    run(modify(Prompt.MultipleChoice.withSomeSelected(label, options)))
end SyncPrompts
