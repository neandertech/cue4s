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

import scala.collection.mutable

class TracingTerminal extends Terminal:
  val WIDTH         = 120
  val HEIGHT        = 500
  var currentHeight = 0
  var currentWidth  = 0
  var currentLine   = 0
  var currentColumn = 0

  var saved = Option.empty[(Int, Int)]

  def set(char: Char, line: Int, column: Int) =
    INTERNAL(WIDTH * line + column) = char

  def currentIndex() = WIDTH * currentLine + currentColumn
  var INTERNAL       = Array.fill[Char](WIDTH * HEIGHT)(' ')

  def updateBounds() =
    currentWidth = currentWidth max currentColumn
    currentHeight = currentHeight max currentLine

  override def screenClear(): this.type =
    INTERNAL = Array.fill[Char](WIDTH * HEIGHT)(' ')
    this

  override def movePreviousLine(n: Int): this.type =
    currentLine = (currentLine - n) max 0
    this

  override def eraseToBeginningOfLine(): this.type =
    for column <- 0 to currentColumn do set(' ', currentLine, column)
    this

  override def eraseToEndOfLine(): this.type =
    for column <- currentColumn to currentWidth do set(' ', currentLine, column)
    this

  override def cursorShow(): this.type = this

  override def moveHorizontalTo(column: Int): this.type =
    currentColumn = column
    updateBounds()
    this

  override def eraseEntireScreen(): this.type = ???

  override def moveToPosition(row: Int, column: Int): this.type =
    currentLine = row - 1
    currentColumn = column - 1
    updateBounds()
    this

  override def eraseToBeginningOfScreen(): this.type = ???

  override def eraseEntireLine(): this.type =
    for column <- 0 to currentWidth do set(' ', currentLine, column)
    this

  override def moveBack(n: Int): this.type =
    errln(s"Back $n characters")
    currentColumn = (currentColumn - n) max 0
    this

  override def moveUp(n: Int): this.type =
    currentLine = (currentLine - n) max 0
    this

  override def save(): this.type =
    saved = Some((currentLine, currentColumn))
    this

  override def cursorHide(): this.type = ???

  override def moveDown(n: Int): this.type =
    currentLine += n
    updateBounds()
    this

  override def eraseToEndOfScreen(): this.type = ???

  override def moveForward(n: Int): this.type =
    currentColumn = (currentColumn + n) max 0
    updateBounds()
    this

  override def moveNextLine(n: Int): this.type =
    currentLine += 1
    currentColumn = 0
    updateBounds()
    this

  override def restore(): this.type =
    saved match
      case None => this
      case Some((line, column)) =>
        currentLine = line
        currentColumn = column
        this

  def getLine(i: Int): String =
    val start = WIDTH * i
    new String(INTERNAL.slice(start, start + currentWidth))

  def writer: String => Unit =

    val simpleWriter: String => Unit = l =>
      errln(s"Writing at $currentLine x $currentColumn: $l")
      val newCurrentWidth = currentWidth max (currentColumn + l.length)
      if newCurrentWidth > WIDTH then todo("line length overflow")
      else
        val start = currentIndex()
        for idx <- 0 until l.length() do INTERNAL(start + idx) = l.charAt(idx)
        currentColumn += l.length()
        currentWidth = newCurrentWidth

    val multilineWriter: String => Unit = l =>
      val lines = l.linesIterator.zipWithIndex.toList
      lines.foreach: (line, idx) =>
        simpleWriter(line)
        if idx != lines.length - 1 then
          currentColumn = 0
          currentLine += 1
        currentHeight = currentHeight max currentLine

    l =>
      if l.contains("\n") then multilineWriter(l)
      else simpleWriter(l)
  end writer

  def get(): String =
    val cur = mutable.StringBuilder()

    for line <- 0 to currentHeight
    do cur.append(getLine(line) + "\n")

    cur.result()

  def getPretty(): String =
    val cur           = mutable.StringBuilder()
    val raw           = get()
    val maxLineLength = raw.linesIterator.map(_.length).maxOption.getOrElse(1)
    cur.append("┏")
    cur.append("━" * maxLineLength)
    cur.append("┓\n")

    raw.linesIterator.zipWithIndex.foreach: (line, idx) =>
      cur.append("┃")
      if idx == currentLine then
        val (pre, after) = line.splitAt(currentColumn)
        cur.append(pre)
        if currentColumn < currentWidth then
          cur.append(
            fansi.Color.Black(fansi.Back.White(line(currentColumn).toString()))
          )
          cur.append(after.drop(1))
      else cur.append(line)
      cur.append("┃")
      cur.append("\n")

    cur.append("┗")
    cur.append("━" * maxLineLength)
    cur.append("┛")

    cur.result()
  end getPretty

end TracingTerminal

def todo(msg: String) = throw new NotImplementedError(s"not implemented: $msg")
