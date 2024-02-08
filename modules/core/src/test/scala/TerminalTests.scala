package proompts

import com.indoorvivants.proompts.*

trait TerminalTests:
  self: munit.FunSuite =>

  def terminalTest(
      name: String
  )(prompt: Prompt, events: List[Event])(implicit loc: munit.Location): Unit =
    test(name) {
      val result =
        terminalSession(
          name,
          prompt,
          events
        )
      assertSnapshot(name, result)
    }

  def terminalSession(name: String, prompt: Prompt, events: List[Event]) =
    val sb        = new java.lang.StringBuilder
    val term      = TracingTerminal(Output.DarkVoid)
    val capturing = Output.Delegate(term.writer, s => sb.append(s + "\n"))

    val i = Interactive(term, prompt, capturing, colors = false)
    events.foreach: ev =>
      sb.append(ev.toString() + "\n")
      i.handler(ev)
      sb.append(term.getPretty() + "\n")
    sb.toString()
  end terminalSession
end TerminalTests
