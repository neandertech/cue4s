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

import cue4s.CharCollector.*

private[cue4s] class KeyboardReadingThread[Result](
    whatNext: Next[Result] => Boolean,
    send: TerminalEvent => Boolean,
    getchar: () => Int,
) extends Thread("cue4s-keyboard-input-thread"):
  override def run(): Unit =
    var lastRead = 0

    inline def read() =
      lastRead = getchar()
      lastRead

    var state = State.Init
    var stop  = false

    while !stop && read() >= 0 do
      if lastRead != 0 then
        try
          val (newState, result) = decode(state, lastRead)

          result match
            case n: DecodeResult => stop = whatNext(n.toNext)
            case e: TerminalEvent =>
              stop = send(e)

          state = newState
        catch
          case ex: CharCollector.ExitThrowable.type =>
            stop = whatNext(Next.Stop)

    end while
  end run
end KeyboardReadingThread
