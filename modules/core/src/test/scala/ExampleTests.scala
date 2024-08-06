package cue4s

import cue4s.*

class ExampleTests extends munit.FunSuite, TerminalTests:
  terminalTest("alternatives.navigation")(
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
    Next.Done("flizza")
  )

  terminalTest("input".only)(
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
    Next.Done("good")
  )

  terminalTest("alternatives.typing")(
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
    Next.Done("flizza")
  )

  terminalTest("multiple.choice")(
    Prompt.MultipleChoice(
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
    Next.Done(List("pizza", "steak", "fried chicken"))
  )

end ExampleTests
