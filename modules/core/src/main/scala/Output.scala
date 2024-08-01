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

package cue4s

trait Output:
  def out[A: AsString](a: A): Unit
  def logLn[A: AsString](a: A): Unit

trait AsString[A]:
  extension (a: A) def render: String

given AsString[String] with
  extension (a: String) def render = a

extension (o: Output)
  def outLn[A: AsString](a: A) =
    o.out(a.render + "\n")
  def loggerOnly: Output =
    new Output:
      override def logLn[A: AsString](a: A): Unit = o.logLn(a)
      override def out[A: AsString](a: A): Unit   = ()

object Output:
  object Std extends PlatformStd

  object StdOut extends Output:
    override def logLn[A: AsString](a: A): Unit = System.out.println(a.render)
    override def out[A: AsString](a: A): Unit   = System.out.print(a.render)

  object DarkVoid extends Output:
    override def logLn[A: AsString](a: A): Unit = ()
    override def out[A: AsString](a: A): Unit   = ()

  class Delegate(writeOut: String => Unit, writeLog: String => Unit)
      extends Output:
    override def logLn[A: AsString](a: A): Unit = writeLog(a.render + "\n")
    override def out[A: AsString](a: A): Unit   = writeOut(a.render)
end Output
