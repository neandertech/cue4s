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

private[cue4s] class Lines:
  private val builder = List.newBuilder[fansi.Str]

  def +=(line: fansi.Str): Unit =
    var start = 0
    var i     = 0
    val len   = line.length
    while i < len do
      val c = line.getChar(i)
      if c == '\n' then
        builder += line.substring(start, i)
        start = i + 1
      else if c == '\r' then
        builder += line.substring(start, i)
        if i + 1 < len && line.getChar(i + 1) == '\n' then i += 1
        start = i + 1
      end if
      i += 1
    end while
    builder += line.substring(start, len)
  end +=

  def +=(line: String): Unit =
    this += fansi.Str(line)

  def result(): List[String] =
    builder.result().map(_.render)

  def result(cols: Int): List[String] =
    val emptyLine = List("")
    builder
      .result()
      .flatMap: line =>
        if line.length > 0 then TextWrap.greedy(line, cols).map(_.render)
        else emptyLine
end Lines
