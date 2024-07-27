package proompts

import proompts.*
import com.indoorvivants.snapshots.munit_integration.*

trait TerminalTests extends MunitSnapshotsIntegration:
  self: munit.FunSuite =>

  def terminalTest[R](
      name: String
  )(prompt: Prompt[R], events: List[Event], expected: Next[R])(implicit
      loc: munit.Location
  ): Unit =
    test(name) {
      val (snapshot, result) =
        terminalSession(
          name,
          prompt,
          events
        )
      assertSnapshot(name, snapshot)
      assertEquals(result, expected)
    }

  def terminalSession[R](name: String, prompt: Prompt[R], events: List[Event]) =
    val sb        = new java.lang.StringBuilder
    val term      = TracingTerminal(Output.DarkVoid)
    val capturing = Output.Delegate(term.writer, s => sb.append(s + "\n"))

    val handler = prompt.handler(term, capturing, colors = false)

    var result = Option.empty[Next[R]]

    events.foreach: ev =>
      sb.append(ev.toString() + "\n")
      result = Some(handler(ev))
      sb.append(term.getPretty() + "\n")

    sb.toString() -> result.getOrElse(sys.error("No result produced"))
  end terminalSession
end TerminalTests
