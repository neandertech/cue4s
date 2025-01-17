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
  self =>
  private[cue4s] def framework(
      terminal: Terminal,
      output: Output,
      theme: Theme,
      symbols: Symbols,
  ): PromptFramework[Result]

  def map[Derived](f: Result => Derived): Prompt[Derived] =
    mapValidated(r => Right(f(r)))

  def mapValidated[Derived](
      f: Result => Either[PromptError, Derived],
  ): Prompt[Derived] =
    new Prompt[Derived]:
      override def framework(
          terminal: Terminal,
          output: Output,
          theme: Theme,
          symbols: Symbols,
      ): PromptFramework[Derived] =
        self.framework(terminal, output, theme, symbols).mapValidated(f)
end Prompt

object Prompt:
  case class Input private (
      lab: String,
      validate: String => Option[PromptError] = _ => None,
      default: Option[String] = None,
  ) extends Prompt[String]:

    def this(lab: String) = this(lab, _ => None)

    def validate(f: String => Option[PromptError]): Input =
      copy(validate = (n: String) => validate(n).orElse(f(n)))

    def default(value: String): Input =
      copy(default = Some(value))

    override def framework(
        terminal: Terminal,
        output: Output,
        theme: Theme,
        symbols: Symbols,
    ) = InteractiveTextInput(
      prompt = lab,
      terminal = terminal,
      out = output,
      theme = theme,
      validate = validate,
      hideText = false,
      symbols = symbols,
      default = default,
    )
  end Input

  object Input:
    def apply(lab: String): Input = new Input(lab)

  import PasswordInput.Password

  case class PasswordInput private (
      private val lab: String,
      private val validate: Password => Option[PromptError] = _ => None,
      private val default: Option[Password] = None,
  ) extends Prompt[Password]:

    def this(lab: String) = this(lab, _ => None)

    def default(value: Password): PasswordInput =
      copy(default = Some(value))

    def validate(f: Password => Option[PromptError]): PasswordInput =
      copy(validate = (n: Password) => validate(n).orElse(f(n)))

    override def framework(
        terminal: Terminal,
        output: Output,
        theme: Theme,
        symbols: Symbols,
    ) =
      val textBase =
        InteractiveTextInput(
          prompt = lab,
          terminal = terminal,
          out = output,
          theme = theme,
          validate = _ => None,
          hideText = true,
          symbols = symbols,
          default = default.map(_.raw),
        )

      textBase.mapValidated[Password](str =>
        val pwd = Password(str)
        validate(pwd).toLeft(pwd),
      )
    end framework

  end PasswordInput

  object PasswordInput:
    case class Password(raw: String):
      override def toString(): String = "Password(***)"

    def apply(lab: String): PasswordInput = new PasswordInput(lab)

  case class NumberInput[N: Numeric] private (
      private val lab: String,
      private val validateNumber: N => Option[PromptError] = (_: N) => None,
      private val default: Option[N] = None,
  ) extends Prompt[N]:
    private val num = Numeric[N]

    def this(lab: String) = this(lab, _ => None)

    def default(value: N): NumberInput[N] = copy(default = Some(value))

    def validate(f: N => Option[PromptError]): NumberInput[N] =
      copy(validateNumber = (n: N) => validateNumber(n).orElse(f(n)))

    def positive = validate(n =>
      Option.when(num.lteq(n, num.zero))(PromptError("must be positive")),
    )

    def negative = validate(n =>
      Option.when(num.lteq(n, num.zero))(PromptError("must be negative")),
    )

    def min(value: N): NumberInput[N] = validate(n =>
      Option.when(num.lt(n, value))(
        PromptError(s"must be no less than $value"),
      ),
    )

    def max(value: N): NumberInput[N] = validate(n =>
      Option.when(num.gt(n, value))(
        PromptError(s"must be no more than $value"),
      ),
    )

    override def framework(
        terminal: Terminal,
        output: Output,
        theme: Theme,
        symbols: Symbols,
    ) =
      val lifted = (n: N) => validateNumber(n).toLeft(n)

      val transform = (s: String) =>
        Numeric[N]
          .parseString(s)
          .toRight(PromptError("not a valid number"))
          .flatMap(lifted)

      val stringValidate = transform(_: String).left.toOption

      InteractiveTextInput(
        prompt = lab,
        terminal = terminal,
        out = output,
        theme = theme,
        validate = stringValidate,
        hideText = false,
        symbols = symbols,
        default = default.map(_.toString()),
      )
        .mapValidated(transform)
    end framework
  end NumberInput

  object NumberInput:
    def apply[N: Numeric](label: String): NumberInput[N] =
      new NumberInput[N](label)

    def int(label: String): NumberInput[Int] = NumberInput[Int](label)

    def float(label: String): NumberInput[Float] = NumberInput[Float](label)

    def double(label: String): NumberInput[Float] = NumberInput[Float](label)
  end NumberInput

  case class Confirmation(lab: String, default: Boolean = true)
      extends Prompt[Boolean]:

    def default(value: Boolean) = copy(default = value)

    override def framework(
        terminal: Terminal,
        output: Output,
        theme: Theme,
        symbols: Symbols,
    ) =
      InteractiveConfirmation(
        prompt = lab,
        default = default,
        terminal = terminal,
        out = output,
        theme = theme,
        symbols = symbols,
      )
  end Confirmation

  object Confirmation:
    def apply(lab: String, default: Boolean = true) =
      new Confirmation(lab, default)
  end Confirmation

  case class SingleChoice(lab: String, alts: List[String], windowSize: Int = 10)
      extends Prompt[String]:
    def withWindowSize(i: Int) = copy(windowSize = i)
    override def framework(
        terminal: Terminal,
        output: Output,
        theme: Theme,
        symbols: Symbols,
    ) =
      InteractiveSingleChoice(
        prompt = this,
        terminal = terminal,
        out = output,
        theme = theme,
        windowSize = windowSize,
        symbols = symbols,
      )
  end SingleChoice

  case class MultipleChoice private (
      lab: String,
      alts: List[(String, Boolean)],
      windowSize: Int,
  ) extends Prompt[List[String]]:

    def withWindowSize(i: Int) = copy(windowSize = i)

    override def framework(
        terminal: Terminal,
        output: Output,
        theme: Theme,
        symbols: Symbols,
    ): PromptFramework[List[String]] =
      InteractiveMultipleChoice(
        prompt = this,
        terminal = terminal,
        out = output,
        theme = theme,
        windowSize = windowSize,
        symbols = symbols,
      )
  end MultipleChoice

  object MultipleChoice:
    @deprecated(
      "This constructor will be removed in the future, use `withNoneSelected` which is equivalent",
    )
    def apply(
        lab: String,
        variants: Seq[String],
        windowSize: Int = 10,
    ): MultipleChoice =
      withNoneSelected(lab, variants, windowSize)

    def withNoneSelected(
        lab: String,
        variants: Seq[String],
        windowSize: Int = 10,
    ) =
      new MultipleChoice(lab, variants.map(_ -> false).toList, windowSize)
    def withAllSelected(
        lab: String,
        variants: Seq[String],
        windowSize: Int = 10,
    ) =
      new MultipleChoice(lab, variants.map(_ -> true).toList, windowSize)
    def withSomeSelected(
        lab: String,
        variants: Seq[(String, Boolean)],
        windowSize: Int = 10,
    ) =
      new MultipleChoice(lab, variants.toList, windowSize)
  end MultipleChoice
end Prompt
