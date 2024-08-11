package cue4s

import cue4s.*

class ExampleTests extends munit.FunSuite, TerminalTests:
  terminalTestComplete("alternatives.navigation")(
    Prompt.SingleChoice(
      "How do you do fellow kids?",
      List("killa", "rizza", "flizza")
    ),
    List(
      Event.Init,
      Event.Key(KeyEvent.DOWN),
      Event.Key(KeyEvent.DOWN),
      Event.Key(KeyEvent.ENTER)
    ),
    "flizza"
  )

  terminalTestComplete("input")(
    Prompt.Input(
      "how do you do fellow kids?",
      value =>
        if value.length < 4 then Some(PromptError("too short!"))
        else None
    ),
    List(
      Event.Init,
      Event.Char('g'),
      Event.Char('o'),
      Event.Key(KeyEvent.ENTER), // prevents submission
      Event.Char('o'),
      Event.Char('d'),
      Event.Key(KeyEvent.ENTER)
    ),
    "good"
  )

  terminalTestComplete("alternatives.typing")(
    Prompt.SingleChoice(
      "How do you do fellow kids?",
      List("killa", "rizza", "flizza")
    ),
    List(
      Event.Init,
      Event.Char('z'),
      Event.Key(KeyEvent.DELETE),
      Event.Char('l'),
      Event.Char('i'),
      Event.Key(KeyEvent.ENTER)
    ),
    "flizza"
  )

  terminalTestComplete("multiple.choice")(
    Prompt.MultipleChoice.withNoneSelected(
      "What would you like for lunch",
      List("pizza", "steak", "sweet potato", "fried chicken")
    ),
    List(
      Event.Init,
      Event.Key(KeyEvent.TAB),
      Event.Key(KeyEvent.DOWN),
      Event.Key(KeyEvent.TAB),
      Event.Key(KeyEvent.DOWN),
      Event.Char('h'), // skip sweet potato
      Event.Key(KeyEvent.TAB),
      Event.Key(KeyEvent.DELETE),
      Event.Key(KeyEvent.ENTER)
    ),
    List("pizza", "steak", "fried chicken")
  )

  terminalTestComplete("multiple.choice.allselected")(
    Prompt.MultipleChoice.withAllSelected(
      "What would you like for lunch",
      List("pizza", "steak", "sweet potato", "fried chicken")
    ),
    List(
      Event.Init,
      Event.Key(KeyEvent.TAB), // unselect pizza
      Event.Key(KeyEvent.ENTER)
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
      Event.Key(KeyEvent.ENTER),
      Event.Init,
      Event.Key(KeyEvent.ENTER)
    ),
    MyPrompt("hello", "yes")
  )

end ExampleTests
