package cue4s

import cue4s.*
import KeyEvent.*

class ExampleTests extends munit.FunSuite, TerminalTests:
  terminalTestComplete("alternatives.navigation")(
    Prompt.SingleChoice(
      "How do you do fellow kids?",
      List("killa", "rizza", "flizza"),
    ),
    list(
      Event.Init,
      DOWN,
      DOWN,
      ENTER,
    ),
    "flizza",
  )

  terminalTest("alternatives.cancel.single")(
    Prompt.SingleChoice(
      "How do you do fellow kids?",
      List("killa", "rizza", "flizza"),
    ),
    list(
      Event.Init,
      DOWN,
      DOWN,
      Event.Interrupt,
    ),
    Next.Stop,
  )

  terminalTestComplete("alternatives.infiniscroll.single")(
    Prompt.SingleChoice(
      "How was your day?",
      List(
        "amazing",
        "productive",
        "relaxing",
        "stressful",
        "exhausting",
      ),
      windowSize = 3,
    ),
    list(
      Event.Init,
      DOWN,
      DOWN,
      DOWN,
      DOWN,
      UP,
      ENTER,
    ),
    "stressful",
  )

  terminalTest("alternatives.cancel.multiple")(
    Prompt.MultipleChoice.withNoneSelected(
      "What would you like for lunch",
      List("pizza", "steak", "sweet potato", "fried chicken"),
    ),
    list(
      Event.Init,
      TAB,
      DOWN,
      TAB,
      DOWN,
      Event.Interrupt,
    ),
    Next.Stop,
  )

  terminalTestComplete("alternatives.infiniscroll.multiple")(
    Prompt.MultipleChoice.withNoneSelected(
      "What would you like for lunch",
      List("pizza", "steak", "sweet potato", "fried chicken", "sushi"),
      windowSize = 3,
    ),
    list(
      Event.Init,
      TAB,
      DOWN,
      TAB,
      DOWN,
      DOWN,
      TAB,
      DOWN,
      TAB,
      ENTER,
    ),
    List("pizza", "steak", "fried chicken", "sushi"),
  )

  terminalTestComplete("alternatives.confirm.default")(
    Prompt.Confirmation("Are you sure?", default = true),
    list(
      Event.Init,
      ENTER,
    ),
    true,
  )

  terminalTestComplete("alternatives.confirm.decline")(
    Prompt.Confirmation("Are you sure?", default = true),
    list(
      Event.Init,
      Event.Char('n'),
    ),
    false,
  )

  terminalTestComplete("alternatives.confirm.agree")(
    Prompt.Confirmation("Are you sure?", default = false),
    list(
      Event.Init,
      Event.Char('y'),
    ),
    true,
  )

  terminalTest("alternatives.confirm.cancel")(
    Prompt.Confirmation("Are you sure?"),
    list(
      Event.Init,
      Event.Interrupt,
    ),
    Next.Stop,
  )

  terminalTest("alternatives.cancel.input")(
    Prompt
      .Input(
        "how do you do fellow kids?",
      )
      .validate(value =>
        if value.length < 4 then Some(PromptError("too short!"))
        else None,
      ),
    list(Event.Init, Event.Interrupt),
    Next.Stop,
  )

  terminalTestComplete("input")(
    Prompt
      .Input("how do you do fellow kids?")
      .validate(value =>
        if value.length < 4 then Some(PromptError("too short!"))
        else None,
      ),
    list(
      Event.Init,
      chars("go"),
      ENTER, // prevents submission
      chars("od"),
      ENTER,
    ),
    "good",
  )

  terminalTestComplete("derived.validated.input")(
    Prompt
      .Input("What color is the sky?")
      .mapValidated:
        case "blue" => Right(true)
        case other  => Left(PromptError(s"look up, it's not $other"))
    ,
    list(
      Event.Init,
      chars("go"),
      ENTER, // prevents submission
      chars("od"),
      ENTER,
      delete("good"),
      chars("blue"),
      ENTER,
    ),
    true,
  )

  terminalTestComplete("number.input")(
    Prompt.NumberInput
      .float("think of a number")
      .min(5.0)
      .max(30.0),
    list(
      Event.Init,
      chars("3.0"),
      ENTER,
      delete("3.0"),
      ENTER,
      chars("h"),
      ENTER,
      delete("h"),
      ENTER,
      chars("31.0"),
      ENTER,
      delete("31.0"),
      chars("25.0"),
      ENTER,
    ),
    25.0f,
  )

  terminalTestComplete("alternatives.typing")(
    Prompt.SingleChoice(
      "How do you do fellow kids?",
      List("killa", "rizza", "flizza"),
    ),
    list(
      Event.Init,
      chars("z"),
      DELETE,
      chars("li"),
      ENTER,
    ),
    "flizza",
  )

  terminalTestComplete("multiple.choice")(
    Prompt.MultipleChoice.withNoneSelected(
      "What would you like for lunch",
      List("pizza", "steak", "sweet potato", "fried chicken"),
    ),
    list(
      Event.Init,
      TAB,
      DOWN,
      TAB,
      DOWN,
      chars("h"), // skip sweet potato
      TAB,
      DELETE,
      ENTER,
    ),
    List("pizza", "steak", "fried chicken"),
  )

  terminalTestComplete("multiple.choice.allselected")(
    Prompt.MultipleChoice.withAllSelected(
      "What would you like for lunch",
      List("pizza", "steak", "sweet potato", "fried chicken"),
    ),
    list(
      Event.Init,
      TAB, // unselect pizza
      ENTER,
    ),
    List("steak", "sweet potato", "fried chicken"),
  )

  case class MyPrompt(
      @cue(_.text("Sir...?"))
      title: String,
      @cue(_.options("yes", "no").text("What's up doc?"))
      lab: String,
  ) derives PromptChain

  terminalTestComplete("promptchain")(
    PromptChain[MyPrompt],
    list(
      Event.Init,
      chars("hello"),
      ENTER,
      Event.Init,
      ENTER,
    ),
    MyPrompt("hello", "yes"),
  )

end ExampleTests
