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

private[cue4s] trait Symbols:
  def promptCancelled: String
  def promptDone: String
  def promptCue: String
  def ellipsis: String
  def pageUpArrow: String
  def pageDownArrow: String
  def altCursor: String
  def altSelected: String
  def altNotSelected: String
end Symbols

private object UnicodeSymbols extends Symbols:

  override val promptCancelled: String = "×"

  override val promptDone: String = "✔"

  override val promptCue: String = "›"

  override val ellipsis: String = "…"

  override val pageUpArrow: String = "↑"

  override val pageDownArrow: String = "↓"

  override val altCursor: String = "‣"

  override val altSelected: String = "◉"

  override val altNotSelected: String = "◯"
end UnicodeSymbols

private object ASCIISymbols extends Symbols:

  override val promptCancelled: String = "X"

  override val promptDone: String = "√"

  override val ellipsis: String = "..."

  override val pageUpArrow: String = "^"

  override val pageDownArrow: String = "v"

  override val altCursor: String = ">"

  override val promptCue = "»"

  override val altSelected: String = "(*)"

  override val altNotSelected: String = "( )"
end ASCIISymbols

private[cue4s] object Symbols:
  lazy val default =
    if Platform.os == Platform.OS.Windows then ASCIISymbols else UnicodeSymbols
