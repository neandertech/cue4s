/*
 * Copyright 2023 Neandertech
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

import java.io.File
import java.io.FileWriter

object FileLogging:
  def toFile(originalOut: Output, file: String): Output =
    createLoggingOut(originalOut, file)

  def fromEnv(originalOut: Output, env: Map[String, String] = sys.env): Output =
    env.get("CUE4S_LOG_FILE") match
      case Some(value) =>
        createLoggingOut(originalOut, value)
      case None =>
        originalOut

  private def createLoggingOut(originalOut: Output, file: String): Output =
    new Output:
      private val fw = new FileWriter(new File(file))
      override def logLn[A: AsString](a: => A): Unit =
        fw.write(a.render + "\n")
        fw.flush()
      override def out[A: AsString](a: A): Unit = originalOut.out(a)
      override def close(): Unit                = fw.close()
end FileLogging
