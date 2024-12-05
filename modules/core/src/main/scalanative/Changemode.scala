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

import cue4s.Platform.OS

import scalanative.unsafe.*
import scalanative.posix.termios.{TCSANOW, ECHO, ICANON}

@extern
def tcgetattr(fd: Int, state: Ptr[Byte]): Int = extern

@extern
def tcsetattr(fd: Int, action: Int, state: Ptr[Byte]): Int = extern

object Changemode:
  def changeMode(rawMode: Boolean) =
    Platform.os match
      case OS.Linux => Linux.changeMode(rawMode)
      case OS.MacOS => MacOS.changeMode(rawMode)
      case OS.Windows =>
        sys.error(
          "Cue4s does not yet support windows: https://github.com/neandertech/cue4s/issues/7",
        )
      case OS.Unknown =>
        sys.error(
          "Cue4s failed to detect the operating system, it is likely unsupported. Please raise an issue (or even a PR!) at https://github.com/neandertech/cue4s",
        )

  object MacOS:
    type termios = scalanative.posix.termios.termios

    var flags = Option.empty[CLong]

    def changeMode(rawMode: Boolean) =
      val state = stackalloc[termios]()

      val STDIN_FILENO = 0
      if rawMode then
        tcgetattr(STDIN_FILENO, state.asInstanceOf)
        this.synchronized:
          flags = Some((!state)._4)
        (!state)._4 = (!state)._4 & ~(ICANON | ECHO)
        assert(tcsetattr(STDIN_FILENO, TCSANOW, state.asInstanceOf) == 0)
      else
        flags.foreach: oldflags =>
          tcgetattr(STDIN_FILENO, state.asInstanceOf)
          (!state)._4 = oldflags
          this.synchronized:
            flags = None
          assert(tcsetattr(STDIN_FILENO, TCSANOW, state.asInstanceOf) == 0)
      end if
    end changeMode
  end MacOS

  object Linux:
    import scalanative.unsafe.Nat.*
    type tcflag_t = CInt /// SIC!!!
    type cc_t     = CChar
    type speed_t  = CInt /// SIC!!!
    type NCCS     = Digit2[_3, _2]
    type c_cc     = CArray[cc_t, NCCS]

    type termios = CStruct7[
      tcflag_t, /* c_iflag - input flags   */
      tcflag_t, /* c_oflag - output flags  */
      tcflag_t, /* c_cflag - control flags */
      tcflag_t, /* c_lflag - local flags   */
      c_cc,     /* cc_t c_cc[NCCS] - control chars */
      speed_t,  /* c_ispeed - input speed   */
      speed_t,  /* c_ospeed - output speed  */
    ]

    var flags = Option.empty[Int]

    def changeMode(rawMode: Boolean) =
      val state = stackalloc[termios]()

      val STDIN_FILENO = 0
      if rawMode then
        tcgetattr(STDIN_FILENO, state.asInstanceOf)
        this.synchronized:
          flags = Some((!state)._4)
        (!state)._4 = (!state)._4 & ~(ICANON | ECHO)
        assert(tcsetattr(STDIN_FILENO, TCSANOW, state.asInstanceOf) == 0)
      else
        flags.foreach: oldflags =>
          tcgetattr(STDIN_FILENO, state.asInstanceOf)
          (!state)._4 = oldflags
          this.synchronized:
            flags = None
          assert(tcsetattr(STDIN_FILENO, TCSANOW, state.asInstanceOf) == 0)
      end if
    end changeMode
  end Linux

end Changemode
