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

trait Terminal:
  self =>
  def cursorShow(): self.type

  def cursorHide(): self.type

  def screenClear(): self.type

  def moveUp(n: Int): self.type

  def moveDown(n: Int): self.type

  def moveForward(n: Int): self.type

  def moveBack(n: Int): self.type

  def moveNextLine(n: Int): self.type

  def movePreviousLine(n: Int): self.type

  def moveHorizontalTo(column: Int): self.type

  def moveToPosition(row: Int, column: Int): self.type

  def eraseToEndOfLine(): self.type

  def eraseToBeginningOfLine(): self.type

  def eraseEntireLine(): self.type

  def eraseToEndOfScreen(): self.type

  def eraseToBeginningOfScreen(): self.type

  def eraseEntireScreen(): self.type

  def save(): self.type

  def restore(): self.type
end Terminal

object Terminal:
  def ansi(writer: String => Unit) = AnsiTerminal(writer)
