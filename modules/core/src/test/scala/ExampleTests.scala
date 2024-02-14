package proompts

import com.indoorvivants.proompts.*

class ExampleTests extends munit.FunSuite, TerminalTests:
  val prompt = Prompt.Alternatives(
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
    )
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
    )
  )

end ExampleTests
