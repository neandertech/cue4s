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

private[cue4s] class StrBuilder private (private var internal: fansi.Str):
  def clear(): Unit =
    internal = fansi.Str("")

  def `++=`(other: fansi.Str): Unit =
    internal ++= other

  def nonEmpty: Boolean =
    internal.length > 0

  def length: Int =
    internal.length

  def `+=`(c: Char): Unit =
    internal ++= fansi.Str(c.toString)

  def result() = fansi.Str(internal.render)
end StrBuilder

private[cue4s] object StrBuilder:
  def apply(): StrBuilder = new StrBuilder(fansi.Str(""))

private[cue4s] object TextWrap:

  private[cue4s] def splitAtWhitespace(text: fansi.Str): List[fansi.Str] =
    val words   = List.newBuilder[fansi.Str]
    var start   = 0
    var inSpace = false
    for
      i <- 0 until text.length
      char = text.getChar(i)
    do
      (char.isWhitespace, inSpace) match
        // consuming whitespace
        case (true, true) =>
          start += 1
        // first non whitespace character after whitespace
        case (false, true) =>
          inSpace = false
        // whitespace char after consuming
        case (true, false) =>
          words += text.substring(start, i)
          inSpace = true
          start = i + 1
        case (false, false) =>
      end match
    end for

    if start != text.length then words += text.substring(start)

    words.result()
  end splitAtWhitespace

  def greedy(text: fansi.Str, maxWidth: Int): List[fansi.Str] =
    val words = splitAtWhitespace(
      text,
    )
    if words.isEmpty then Nil
    else
      val result = List.newBuilder[fansi.Str]
      val line   = StrBuilder()

      def emitWord(word: fansi.Str): Unit =
        if word.length <= maxWidth then
          if line.nonEmpty && line.length + 1 + word.length > maxWidth then
            result += line.result()
            line.clear()
            line ++= word
          else
            if line.nonEmpty then line += ' '
            line ++= word
        else
          var remaining = word
          if line.nonEmpty then
            val avail = maxWidth - line.length - 1
            if avail > 0 then
              val (head, tail) = remaining.splitAt(avail)
              line += ' '
              line ++= head
              remaining = tail
            end if
            result += line.result()
            line.clear()
          end if

          while remaining.length > maxWidth do
            val (head, tail) = remaining.splitAt(maxWidth)
            result += head
            remaining = tail

          if remaining.length > 0 then line ++= remaining

      for word <- words do emitWord(word)

      if line.nonEmpty then result += line.result()
      result.result()
    end if
  end greedy
end TextWrap
