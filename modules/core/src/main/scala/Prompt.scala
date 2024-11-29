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
      ): PromptFramework[Derived] =
        self.framework(terminal, output, theme).mapValidated(f)
end Prompt

object Prompt:
  case class Input private (
      lab: String,
      validate: String => Option[PromptError] = _ => None,
  ) extends Prompt[String]:

    def this(lab: String) = this(lab, _ => None)

    def validate(f: String => Option[PromptError]): Input =
      copy(validate = (n: String) => validate(n).orElse(f(n)))

    override def framework(
        terminal: Terminal,
        output: Output,
        theme: Theme,
    ) = InteractiveTextInput(lab, terminal, output, theme, validate)
  end Input

  object Input:
    def apply(lab: String): Input = new Input(lab)

  case class NumberInput[N: Numeric] private (
      lab: String,
      validateNumber: N => Option[PromptError] = (_: N) => None,
  ) extends Prompt[N]:
    private val num = Numeric[N]

    def this(lab: String) = this(lab, _ => None)

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
    ) =
      val lifted = (n: N) => validateNumber(n).toLeft(n)

      val transform = (s: String) =>
        Numeric[N]
          .parseString(s)
          .toRight(PromptError("not a valid number"))
          .flatMap(lifted)

      val stringValidate = transform(_: String).left.toOption

      InteractiveTextInput(lab, terminal, output, theme, stringValidate)
        .mapValidated(transform)
    end framework
  end NumberInput

  object NumberInput:
    val int    = NumberInput[Int].apply(_, _ => None)
    val float  = NumberInput[Float].apply(_, _ => None)
    val double = NumberInput[Float].apply(_, _ => None)
  case class SingleChoice(lab: String, alts: List[String], windowSize: Int = 10)
      extends Prompt[String]:
    override def framework(
        terminal: Terminal,
        output: Output,
        theme: Theme,
    ) =
      InteractiveSingleChoice(
        this,
        terminal,
        output,
        theme,
        windowSize,
      )
  end SingleChoice

  case class MultipleChoice private (
      lab: String,
      alts: List[(String, Boolean)],
      windowSize: Int,
  ) extends Prompt[List[String]]:
    override def framework(
        terminal: Terminal,
        output: Output,
        theme: Theme,
    ): PromptFramework[List[String]] =
      InteractiveMultipleChoice(
        this,
        terminal,
        output,
        theme,
        windowSize,
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
