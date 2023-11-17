/*
 * Copyright 2020 Anton Sviridov
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

private[proompts] object ANSI:
  final val ESC = '\u001b'
  final val CSI = s"$ESC["

  inline def call(name: Char, inline args: Int*) =
    s"$CSI${args.mkString(";")}$name"

  inline def m(args: Int*) =
    call('m', args*)

  object cursor:
    inline def show() =
      s"$CSI?25h"

    inline def hide() =
      s"$CSI?25l"

  object screen:
    inline def clear() =
      s"${ESC}c"

  object move:
    inline def up(n: Int) =
      call('A', n)

    inline def down(n: Int) =
      call('B', n)

    inline def forward(n: Int) =
      call('C', n)

    inline def back(n: Int) =
      call('D', n)

    inline def nextLine(n: Int) =
      call('E', n)

    inline def previousLine(n: Int) =
      call('F', n)

    inline def horizontalTo(column: Int) =
      call('G', column)

    inline def position(row: Int, column: Int) =
      call('H', row, column)
  end move

  object erase:
    object line:
      inline def apply(n: Int) =
        call('K', n)

      inline def toEndOfLine() =
        apply(0)

      inline def toBeginningOfLine() =
        apply(1)

      inline def entireLine() =
        apply(2)
    end line

    object display:
      inline def apply(n: Int) =
        call('J', n)

      inline def toEndOfScreen() =
        apply(0)

      inline def toBeinningOfScreen() =
        apply(1)

      inline def entireScreen() =
        apply(2)
    end display
  end erase

  inline def save() =
    call('s')

  inline def restore() =
    call('u')

  inline def withRestore[A](writer: String => Unit)(inline f: => A) =
    writer(save())
    f
    writer(restore())

end ANSI
