package cue4s

import cue4s.*
import com.indoorvivants.snapshots.munit_integration.*

trait TerminalTests extends MunitSnapshotsIntegration:
  self: munit.FunSuite =>

  case object LogTag extends munit.Tag("logtest")

  def terminalTest[R](
      name: munit.TestOptions
  )(prompt: Prompt[R], events: List[Event], expected: Next[R])(implicit
      loc: munit.Location
  ): Unit =
    test(name) {
      val (snapshot, result) =
        terminalSession(
          name.name,
          prompt,
          events
        )

      assertSnapshot(name.name, snapshot)
      assertEquals(result, expected)
    }

  def terminalSession[R](
      name: String,
      prompt: Prompt[R],
      events: List[Event],
      log: Boolean = false
  ) =
    val sb = new java.lang.StringBuilder
    val logger: String => Unit =
      if log then s => sb.append(s + "\n") else _ => ()
    val term      = TracingTerminal(Output.Delegate(_ => (), logger))
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
