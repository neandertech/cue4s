/*
 * Copyright 2023 Anton Sviridov
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

package com.indoorvivants.proompts

@main def hello =

  def testingProgram(
      terminal: Terminal,
      events: List[(Event, () => Unit)],
      out: Output
  ) =
    val i = InteractiveAlternatives(
      terminal,
      Prompt.Alternatives(
        "How do you do fellow kids?",
        List("killa", "rizza", "flizza")
      ),
      out,
      colors = false
    )

    events.foreach: (ev, callback) =>
      i.handler(ev)
      callback()
  end testingProgram

  val term      = TracingTerminal(Output.DarkVoid)
  val capturing = Output.Delegate(term.writer, s => Output.StdOut.logLn(s))

  val events =
    List(
      Event.Init,
      Event.Key(KeyEvent.DOWN),
      Event.Char('r'),
      Event.Key(KeyEvent.DELETE),
      Event.Char('l'),
      Event.Key(KeyEvent.DOWN),
      Event.Key(KeyEvent.UP),
      Event.Char('i')
    )

  testingProgram(
    term,
    events
      .map(ev =>
        ev -> { () =>
          println(ev); println(term.getPretty())
        }
      ),
    capturing
  )

end hello
