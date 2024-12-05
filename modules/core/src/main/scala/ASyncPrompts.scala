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

import cue4s.Prompt.PasswordInput.Password

class AsyncPromptsBuilder private (impl: AsyncPromptsOptions):
  def this() = this(AsyncPromptsOptions(Output.Std, Theme.Default))

  def noColors: AsyncPromptsBuilder                = withTheme(Theme.NoColors)
  def withOutput(out: Output): AsyncPromptsBuilder = copy(_.copy(out = out))
  def withTheme(theme: Theme): AsyncPromptsBuilder = copy(_.copy(theme = theme))

  def use[A](f: AsyncPrompts => A)(using ExecutionContext): A =
    val p    = Prompts(impl.out, Terminal.ansi, impl.theme)
    val inst = AsyncPrompts(p)
    try
      val result = f(inst)
      result match
        case f: Future[?] =>
          // Only important on JS, where finalizer might not run
          f.onComplete(_ => p.close())
        case _ =>

      result
    finally p.close()
    end try
  end use

  private def copy(f: AsyncPromptsOptions => AsyncPromptsOptions) =
    new AsyncPromptsBuilder(f(impl))
end AsyncPromptsBuilder

private case class AsyncPromptsOptions(out: Output, theme: Theme)

class AsyncPrompts(underlying: Prompts):
  export underlying.runAsync

  def text(
      label: String,
      modify: Prompt.Input => Prompt.Input = identity,
  )(using ExecutionContext): Future[Completion[String]] =
    runAsync(modify(Prompt.Input(label)))

  def password(
      label: String,
      modify: Prompt.PasswordInput => Prompt.PasswordInput = identity,
  )(using ExecutionContext): Future[Completion[Password]] =
    runAsync(modify(Prompt.PasswordInput(label)))

  def int(
      label: String,
      modify: Prompt.NumberInput[Int] => Prompt.NumberInput[Int] = identity,
  )(using ExecutionContext): Future[Completion[Int]] =
    runAsync(modify(Prompt.NumberInput(label)))

  def float(
      label: String,
      modify: Prompt.NumberInput[Float] => Prompt.NumberInput[Float] = identity,
  )(using ExecutionContext): Future[Completion[Float]] =
    runAsync(modify(Prompt.NumberInput(label)))

  def double(
      label: String,
      modify: Prompt.NumberInput[Double] => Prompt.NumberInput[Double] =
        identity,
  )(using ExecutionContext): Future[Completion[Double]] =
    runAsync(modify(Prompt.NumberInput(label)))

  def singleChoice(
      label: String,
      options: List[String],
      modify: Prompt.SingleChoice => Prompt.SingleChoice = identity,
  )(using ExecutionContext): Future[Completion[String]] =
    runAsync(modify(Prompt.SingleChoice(label, options)))

  def confirm(
      label: String,
      modify: Prompt.Confirmation => Prompt.Confirmation = identity,
      default: Boolean = true,
  )(using ExecutionContext): Future[Completion[Boolean]] =
    runAsync(modify(Prompt.Confirmation(label, default)))

  def multiChoiceAllSelected(
      label: String,
      options: List[String],
      modify: Prompt.MultipleChoice => Prompt.MultipleChoice = identity,
  )(using ExecutionContext): Future[Completion[List[String]]] =
    runAsync(modify(Prompt.MultipleChoice.withAllSelected(label, options)))

  def multiChoiceNoneSelected(
      label: String,
      options: List[String],
      modify: Prompt.MultipleChoice => Prompt.MultipleChoice = identity,
  )(using ExecutionContext): Future[Completion[List[String]]] =
    runAsync(modify(Prompt.MultipleChoice.withNoneSelected(label, options)))

  def multiChoiceSomeSelected(
      label: String,
      options: List[(String, Boolean)],
      modify: Prompt.MultipleChoice => Prompt.MultipleChoice = identity,
  )(using ExecutionContext): Future[Completion[List[String]]] =
    runAsync(modify(Prompt.MultipleChoice.withSomeSelected(label, options)))
end AsyncPrompts
