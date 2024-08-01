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

import scala.concurrent.Future
import scala.util.boundary

import scalanative.libc.stdio.getchar
import scalanative.unsafe.*
import scalanative.posix.termios.*
import boundary.break
import CharCollector.*

def changemode(rawMode: Boolean) =
  val oldt         = stackalloc[termios]()
  val newt         = stackalloc[termios]()
  val STDIN_FILENO = 0
  if rawMode then
    tcgetattr(STDIN_FILENO, oldt)
    !newt = !oldt
    (!newt)._4 = (!newt)._4 & ~(ICANON | ECHO)
    tcsetattr(STDIN_FILENO, TCSANOW, newt)
  else tcsetattr(STDIN_FILENO, TCSANOW, oldt)
end changemode

private class InputProviderImpl(o: Output)
    extends InputProvider(o),
      InputProviderPlatform:

  override def evaluateFuture[Result](handler: Handler[Result]) =
    Future.successful(evaluate(handler))

  override def evaluate[Result](handler: Handler[Result]): Completion[Result] =
    changemode(rawMode = true)

    var lastRead = 0

    inline def read() =
      lastRead = getchar()
      lastRead

    boundary[Completion[Result]]:

      def whatNext(n: Next[Result]) =
        n match
          case Next.Continue    =>
          case Next.Done(value) => break(Completion.Finished(value))
          case Next.Stop        => break(Completion.Interrupted)
          case Next.Error(msg)  => break(Completion.Error(msg))

      def send(ev: Event) =
        whatNext(handler(ev))

      var state = State.Init

      whatNext(handler(Event.Init))

      while read() != 0 do
        val (newState, result) = decode(state, lastRead)

        result match
          case n: DecodeResult => whatNext(n.toNext)
          case e: Event =>
            send(e)

        state = newState

      end while

      Completion.Interrupted

  end evaluate

  override def close() = changemode(rawMode = false)
end InputProviderImpl
