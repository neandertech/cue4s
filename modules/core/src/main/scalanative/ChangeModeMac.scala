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

import scalanative.unsafe.*
import cue4s.ChangeModeUnix.Termios

object ChangeModeMac extends ChangeModeUnix:
  import scalanative.posix.termios.{TCSANOW, ECHO, ICANON}

  type termios = scalanative.posix.termios.termios

  var flags = Option.empty[CLong]

  def changeMode(rawMode: Boolean): Boolean =
    val state = stackalloc[termios]()

    val STDIN_FILENO = 0
    if rawMode then
      Termios.tcgetattr(STDIN_FILENO, state.asInstanceOf)
      this.synchronized:
        flags = Some((!state)._4)
      (!state)._4 = (!state)._4 & ~(ICANON | ECHO)
      Termios.tcsetattr(STDIN_FILENO, TCSANOW, state.asInstanceOf) == 0
    else
      flags.foreach: oldflags =>
        Termios.tcgetattr(STDIN_FILENO, state.asInstanceOf)
        (!state)._4 = oldflags
        this.synchronized:
          flags = None
      Termios.tcsetattr(STDIN_FILENO, TCSANOW, state.asInstanceOf) == 0
    end if
  end changeMode

end ChangeModeMac
