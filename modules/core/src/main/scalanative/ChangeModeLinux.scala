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

import cue4s.ChangeModeUnix.Termios

import scalanative.unsafe.*

object ChangeModeLinux extends ChangeModeUnix:

  import scalanative.posix.termios.{TCSANOW, ECHO, ICANON}
  import scalanative.unsafe.Nat.*
  private type tcflag_t = CInt /// SIC!!!
  private type cc_t     = CChar
  private type speed_t  = CInt /// SIC!!!
  private type NCCS     = Digit2[_3, _2]
  private type c_cc     = CArray[cc_t, NCCS]

  private type termios = CStruct7[
    tcflag_t, /* c_iflag - input flags   */
    tcflag_t, /* c_oflag - output flags  */
    tcflag_t, /* c_cflag - control flags */
    tcflag_t, /* c_lflag - local flags   */
    c_cc,     /* cc_t c_cc[NCCS] - control chars */
    speed_t,  /* c_ispeed - input speed   */
    speed_t,  /* c_ospeed - output speed  */
  ]

  private var flags = Option.empty[Int]

  def changeMode(rawMode: Boolean): Boolean =
    Zone:
      val state = alloc[termios]()

      val isTTY = scalanative.posix.unistd.isatty(STDIN_FILENO) == 1

      if isTTY then
        if rawMode then
          assertAndReturn(
            Termios.tcgetattr(STDIN_FILENO, state.asInstanceOf) == 0,
            "getting current flags failed",
          )
          flags = Some((!state)._4)
          (!state)._4 = (!state)._4 & ~(ICANON | ECHO)
          assertAndReturn(
            Termios
              .tcsetattr(STDIN_FILENO, TCSANOW, state.asInstanceOf) == 0,
            "changing to char input failed",
          )
        else
          assertAndReturn(
            Termios.tcgetattr(STDIN_FILENO, state.asInstanceOf) == 0,
            "getting current flags failed",
          )
          flags.foreach: old =>
            state._4 = old
            flags = None
          assertAndReturn(
            Termios
              .tcsetattr(STDIN_FILENO, TCSANOW, state.asInstanceOf) == 0,
            "changing back from char input failed",
          )
        end if
      else false
      end if
  end changeMode

end ChangeModeLinux
