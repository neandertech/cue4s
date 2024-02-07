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

  def testingProgram(terminal: Terminal, write: String => Unit) =
    write("hello")
    write("worldasdasdasd\ntest")
    write("yo")
    write("bla")
    write("s")
    terminal.moveBack(3)
    write("kkkk")
    write("kkkk\nhhhh")

  val term   = TracingTerminal()
  val writer = term.writer

  testingProgram(term, writer)

  println(term.getPretty())

  // changemode(1)
  val writer1 = (s: String) => System.out.print(s)
  val ansi    = Terminal.ansi(writer1)

  testingProgram(ansi, writer1)

  // testingProgram()

  // val term = TracingTerminal()

  // val write = term.writer

  // write("hello")
  // write("worldasdasdasd\ntest")
  // write("yo")
  // write("bla")
  // write("s")

  // println(term.getPretty())

  // term.moveBack(3)

  // write("wazooop")

  // println(term.getPretty())
  // write("world\n")
  // write("yo")

  // val prompt = Prompt.Alternatives(
  //   "How would you describe yourself?",
  //   List("Sexylicious", "Shmexy", "Pexying")
  // )

  // println(
  //   InputProvider().attach(env => Interactive(prompt, env.writer).handler)
  // )

end hello
