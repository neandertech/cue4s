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

object TextWrap:
  def greedy(text: String, maxWidth: Int): List[String] =
    val words = text.split("\\s+").filter(_.nonEmpty).toList
    if words.isEmpty then Nil
    else
      val result = List.newBuilder[String]
      val line   = StringBuilder()

      def emitWord(word: String): Unit =
        if word.length <= maxWidth then
          if line.nonEmpty && line.length + 1 + word.length > maxWidth then
            result += line.toString
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
            result += line.toString
            line.clear()
          end if

          while remaining.length > maxWidth do
            val (head, tail) = remaining.splitAt(maxWidth)
            result += head
            remaining = tail

          if remaining.nonEmpty then line ++= remaining

      for word <- words do emitWord(word)

      if line.nonEmpty then result += line.toString
      result.result()
    end if
  end greedy
end TextWrap
