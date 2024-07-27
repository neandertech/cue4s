package proompts

import proompts.*

class ExampleTests extends munit.FunSuite, TerminalTests:
  val prompt = AlternativesPrompt(
    "How do you do fellow kids?",
    List("killa", "rizza", "flizza")
  )

  terminalTest("alternatives.navigation")(
    prompt,
    List(
      Event.Init,
      Event.Key(KeyEvent.DOWN),
      Event.Key(KeyEvent.DOWN),
      Event.Key(KeyEvent.ENTER)
    ),
    Next.Done("flizza")
  )

  terminalTest("input")(
    InputPrompt("how do you do fellow kids?"),
    List(
      Event.Init,
      Event.Char('g'),
      Event.Char('o'),
      Event.Char('o'),
      Event.Char('d'),
      Event.Key(KeyEvent.ENTER)
    ),
    Next.Done("good")
  )

  terminalTest("alternatives.typing")(
    prompt,
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

end ExampleTests
