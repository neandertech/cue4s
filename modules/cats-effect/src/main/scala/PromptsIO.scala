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

package cue4s.catseffect

import cats.effect.*
import cue4s.*
import cue4s.Prompt.PasswordInput.Password

class PromptsIO private (
    protected val out: Output,
    protected val terminal: Terminal,
    protected val theme: Theme,
) extends AutoCloseable:
  protected lazy val inputProvider = InputProvider(terminal)

  def run[A](
      prompt: Prompt[A],
  ): IO[Completion[A]] =
    val inputProvider = InputProvider(terminal)
    val framework     = prompt.framework(terminal, out, theme)

    // TODO: provide native CE interface here
    IO.executionContext
      .flatMap: ec =>
        IO.fromFuture(
          IO(inputProvider.evaluateFuture(framework.handler)(using ec)),
        )
      .guarantee(IO(terminal.cursorShow()))
      .guarantee(IO(inputProvider.close()))

  end run

  @deprecated(
    "use `.run(...)` instead, this method will be removed in 0.1.0",
    "0.0.5",
  )
  def io[A](
      prompt: Prompt[A],
  ): IO[Completion[A]] = run(prompt)

  def text(
      label: String,
      modify: Prompt.Input => Prompt.Input = identity,
  ): IO[Completion[String]] =
    run(modify(Prompt.Input(label)))

  def password(
      label: String,
      modify: Prompt.PasswordInput => Prompt.PasswordInput = identity,
  ): IO[Completion[Password]] =
    run(modify(Prompt.PasswordInput(label)))

  def int(
      label: String,
      modify: Prompt.NumberInput[Int] => Prompt.NumberInput[Int] = identity,
  ): IO[Completion[Int]] =
    run(modify(Prompt.NumberInput(label)))

  def float(
      label: String,
      modify: Prompt.NumberInput[Float] => Prompt.NumberInput[Float] = identity,
  ): IO[Completion[Float]] =
    run(modify(Prompt.NumberInput(label)))

  def double(
      label: String,
      modify: Prompt.NumberInput[Double] => Prompt.NumberInput[Double] =
        identity,
  ): IO[Completion[Double]] =
    run(modify(Prompt.NumberInput(label)))

  def singleChoice(
      label: String,
      options: List[String],
      modify: Prompt.SingleChoice => Prompt.SingleChoice = identity,
  ): IO[Completion[String]] =
    run(modify(Prompt.SingleChoice(label, options)))

  def confirm(
      label: String,
      modify: Prompt.Confirmation => Prompt.Confirmation = identity,
      default: Boolean = true,
  ): IO[Completion[Boolean]] =
    run(modify(Prompt.Confirmation(label, default)))

  def multiChoiceAllSelected(
      label: String,
      options: List[String],
      modify: Prompt.MultipleChoice => Prompt.MultipleChoice = identity,
  ): IO[Completion[List[String]]] =
    run(modify(Prompt.MultipleChoice.withAllSelected(label, options)))

  def multiChoiceNoneSelected(
      label: String,
      options: List[String],
      modify: Prompt.MultipleChoice => Prompt.MultipleChoice = identity,
  ): IO[Completion[List[String]]] =
    run(modify(Prompt.MultipleChoice.withNoneSelected(label, options)))

  def multiChoiceSomeSelected(
      label: String,
      options: List[(String, Boolean)],
      modify: Prompt.MultipleChoice => Prompt.MultipleChoice = identity,
  ): IO[Completion[List[String]]] =
    run(modify(Prompt.MultipleChoice.withSomeSelected(label, options)))

  override def close(): Unit = inputProvider.close()
end PromptsIO

object PromptsIO:
  @deprecated(
    "use `PromptsIO.make` or `PromptsIO.builder.make` instead, this method will be removed in 0.1.0",
    "0.0.5",
  )
  def apply(
      out: Output = Output.Std,
      createTerminal: Output => Terminal = Terminal.ansi,
      theme: Theme = Theme.Default,
  ): Resource[IO, PromptsIO] = Resource.fromAutoCloseable(
    IO(new PromptsIO(out, createTerminal(out), theme)),
  )

  def builder: PromptsIOBuilder = PromptsIOBuilder()

  def make: Resource[IO, PromptsIO] = builder.make
end PromptsIO

class PromptsIOBuilder private (impl: IOPromptsOptions):
  def this() = this(IOPromptsOptions(Output.Std, Theme.Default))

  def noColors: PromptsIOBuilder                = withTheme(Theme.NoColors)
  def withOutput(out: Output): PromptsIOBuilder = copy(_.copy(out = out))
  def withTheme(theme: Theme): PromptsIOBuilder = copy(_.copy(theme = theme))

  def make: Resource[IO, PromptsIO] =
    PromptsIO(impl.out, Terminal.ansi, impl.theme)

  private def copy(f: IOPromptsOptions => IOPromptsOptions) =
    new PromptsIOBuilder(f(impl))
end PromptsIOBuilder

private case class IOPromptsOptions(out: Output, theme: Theme)
