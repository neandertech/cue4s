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

private[cue4s] enum Event:
  case Init
  case Key(which: KeyEvent)
  case Char(which: Int)
  case CSICode(bytes: List[Byte])
  case Interrupt

  override def toString(): String =
    this match
      case Init           => "Event.Init"
      case Key(which)     => s"Event.Key($which)"
      case Char(which)    => s"Event.Char('${which.toChar}')"
      case CSICode(bytes) => s"Event.CSICode(${bytes.mkString("[", ", ", "]")})"
      case Interrupt      => "Event.Interrupt"
end Event

private[cue4s] object Event:
  object Char:
    def apply(c: scala.Char): Event.Char = Event.Char(c.toInt)

private[cue4s] enum KeyEvent:
  case UP, DOWN, LEFT, RIGHT, ENTER, DELETE, TAB
