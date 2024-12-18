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

class AnsiTerminal(out: Output) extends Terminal:
  import AnsiTerminal.{ESC, CSI}

  private val writer = (s: String) => out.out(s)

  private inline def call(name: Char, inline args: Int*): this.type =
    writer(s"$CSI${args.mkString(";")}$name")
    this

  private inline def call(v: String): this.type =
    writer(v)
    this

  override inline def cursorHide(): this.type = call(s"$CSI?25l")
  override inline def cursorShow(): this.type = call(s"$CSI?25h")

  private inline def lineEraseMode(n: Int): this.type =
    call('K', n)

  private inline def screenEraseMode(n: Int): this.type =
    call('K', n)

  override inline def eraseEntireLine(): this.type   = lineEraseMode(2)
  override inline def eraseEntireScreen(): this.type = screenEraseMode(2)

  override inline def eraseToBeginningOfLine(): this.type   = lineEraseMode(1)
  override inline def eraseToBeginningOfScreen(): this.type = screenEraseMode(1)

  override inline def eraseToEndOfLine(): this.type   = lineEraseMode(0)
  override inline def eraseToEndOfScreen(): this.type = screenEraseMode(0)

  override inline def moveBack(n: Int): this.type    = call('D', n)
  override inline def moveDown(n: Int): this.type    = call('B', n)
  override inline def moveForward(n: Int): this.type = call('C', n)
  override inline def moveHorizontalTo(column: Int): this.type =
    call('G', column)
  override inline def moveNextLine(n: Int): this.type     = call('E', n)
  override inline def movePreviousLine(n: Int): this.type = call('F', n)
  override inline def moveToPosition(row: Int, column: Int): this.type =
    call('H', row, column)
  override inline def moveUp(n: Int): this.type =
    call('A', n)

  override inline def restore(): this.type     = call('u')
  override inline def save(): this.type        = call('s')
  override inline def screenClear(): this.type = call(s"${ESC}c")

end AnsiTerminal

object AnsiTerminal:
  final val ESC = '\u001b'
  final val CSI = s"$ESC["
