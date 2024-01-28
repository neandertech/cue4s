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
  val term = TracingTerminal()

  val write = term.writer

  write("hello")
  write("world\ntest")
  write("yo")
  write("bla")
  // write("world\n")
  // write("yo")

  // val prompt = Prompt.Alternatives(
  //   "How would you describe yourself?",
  //   List("Sexylicious", "Shmexy", "Pexying")
  // )

  // println(
  //   InputProvider().attach(env => Interactive(prompt, env.writer).handler)
  // )

  println(term.getPretty())

end hello
