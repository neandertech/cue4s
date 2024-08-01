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

import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.annotation.JSImport

import scalajs.js

@js.native
trait ReadStream extends js.Object:
  def isRaw: js.UndefOr[Boolean] = js.native
  def isTTY: js.UndefOr[Boolean] = js.native

  def setRawMode(mode: Boolean): ReadStream = js.native

  def on(eventName: String, listener: js.Function): this.type = js.native
  def removeListener(eventName: String, listener: js.Function): this.type =
    js.native
end ReadStream

@js.native
trait WriteStream extends js.Object:
  def write(a: Any): Unit = js.native

@js.native
trait Process extends js.Object:
  def stdin: ReadStream   = js.native
  def stderr: WriteStream = js.native
  def stdout: WriteStream = js.native

@js.native
@JSGlobal("process")
object Process extends Process

@js.native
trait ReadlineInterface extends js.Object:
  def close(): Unit = js.native

@js.native
trait Readline extends js.Object:
  def createInterface(options: js.Object): ReadlineInterface = js.native
  def emitKeypressEvents(in: ReadStream, rl: ReadlineInterface): Unit =
    js.native

@js.native
@JSImport("readline", JSImport.Namespace)
object Readline extends Readline

@js.native
trait Cons extends js.Object:
  def log(j: js.Any*): Unit = js.native

@js.native
@JSGlobal("console")
object Cons extends Cons

@js.native
trait Key extends js.Object:
  def sequence: String = js.native
  def ctrl: Boolean    = js.native
  def shift: Boolean   = js.native
  def name: String     = js.native
