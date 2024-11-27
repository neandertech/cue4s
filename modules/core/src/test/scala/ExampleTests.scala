package cue4s

import cue4s.*
import KeyEvent.*

class ExampleTests extends munit.FunSuite, TerminalTests:
  terminalTestComplete("alternatives.navigation")(
    Prompt.SingleChoice(
      "How do you do fellow kids?",
      List("killa", "rizza", "flizza")
    ),
    list(
      Event.Init,
      DOWN,
      DOWN,
      ENTER
    ),
    "flizza"
  )

  terminalTest("alternatives.cancel.single")(
    Prompt.SingleChoice(
      "How do you do fellow kids?",
      List("killa", "rizza", "flizza")
    ),
    list(
      Event.Init,
      DOWN,
      DOWN,
      Event.Interrupt
    ),
    Next.Stop
  )

  terminalTestComplete("alternatives.infiniscroll.single")(
    Prompt.SingleChoice(
      "How was your day?",
      List(
        "amazing",
        "productive",
        "relaxing",
        "stressful",
        "exhausting"
      ),
      windowSize = 3
    ),
    list(
      Event.Init,
      DOWN,
      DOWN,
      DOWN,
      DOWN,
      UP,
      ENTER
    ),
    "stressful"
  )

  terminalTest("alternatives.cancel.multiple")(
    Prompt.MultipleChoice.withNoneSelected(
      "What would you like for lunch",
      List("pizza", "steak", "sweet potato", "fried chicken")
    ),
    list(
      Event.Init,
      TAB,
      DOWN,
      TAB,
      DOWN,
      Event.Interrupt
    ),
    Next.Stop
  )

  terminalTestComplete("alternatives.infiniscroll.multiple")(
    Prompt.MultipleChoice.withNoneSelected(
      "What would you like for lunch",
      List("pizza", "steak", "sweet potato", "fried chicken", "sushi"),
      windowSize = 3
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
      ENTER
    ),
    List("pizza", "steak", "fried chicken", "sushi")
  )

  terminalTest("alternatives.cancel.input")(
    Prompt.Input(
      "how do you do fellow kids?",
      value =>
        if value.length < 4 then Some(PromptError("too short!"))
        else None
    ),
    list(Event.Init, Event.Interrupt),
    Next.Stop
  )

  terminalTestComplete("input")(
    Prompt.Input(
      "how do you do fellow kids?",
      value =>
        if value.length < 4 then Some(PromptError("too short!"))
        else None
    ),
    list(
      Event.Init,
      chars("go"),
      ENTER, // prevents submission
      chars("od"),
      ENTER
    ),
    "good"
  )

  terminalTestComplete("alternatives.typing")(
    Prompt.SingleChoice(
      "How do you do fellow kids?",
      List("killa", "rizza", "flizza")
    ),
    list(
      Event.Init,
      chars("z"),
      DELETE,
      chars("li"),
      ENTER
    ),
    "flizza"
  )

  terminalTestComplete("multiple.choice")(
    Prompt.MultipleChoice.withNoneSelected(
      "What would you like for lunch",
      List("pizza", "steak", "sweet potato", "fried chicken")
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
      ENTER
    ),
    List("pizza", "steak", "fried chicken")
  )

  terminalTestComplete("multiple.choice.allselected")(
    Prompt.MultipleChoice.withAllSelected(
      "What would you like for lunch",
      List("pizza", "steak", "sweet potato", "fried chicken")
    ),
    list(
      Event.Init,
      TAB, // unselect pizza
      ENTER
    ),
    List("steak", "sweet potato", "fried chicken")
  )

  case class MyPrompt(
      @cue(_.text("Sir...?"))
      title: String,
      @cue(_.options("yes", "no").text("What's up doc?"))
      lab: String
  ) derives PromptChain

  terminalTestComplete("promptchain")(
    PromptChain[MyPrompt],
    list(
      Event.Init,
      chars("hello"),
      ENTER,
      Event.Init,
      ENTER
    ),
    MyPrompt("hello", "yes")
  )

end ExampleTests
