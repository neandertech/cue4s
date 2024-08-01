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

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.boundary

import boundary.break
import CharCollector.*

private class InputProviderImpl(o: Output)
    extends InputProvider(o),
      InputProviderPlatform:

  override def evaluateFuture[Result](handler: Handler[Result])(using
      ExecutionContext
  ) =
    Future(evaluate(handler))
  end evaluateFuture

  override def evaluate[Result](handler: Handler[Result]): Completion[Result] =
    cue4s.ChangeMode.changemode(1)

    var lastRead = 0

    inline def read() =
      lastRead = cue4s.ChangeMode.CLibrary.INSTANCE.getchar()
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

  override def close() = cue4s.ChangeMode.changemode(0)

end InputProviderImpl
