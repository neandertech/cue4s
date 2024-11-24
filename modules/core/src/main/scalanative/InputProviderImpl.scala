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

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.boundary

import scalanative.libc.stdio.getchar
import scalanative.unsafe.*
import scalanative.posix.termios.*
import boundary.break
import CharCollector.*

private class InputProviderImpl(o: Output)
    extends InputProvider(o),
      InputProviderPlatform:

  private val rt                     = Runtime.getRuntime()
  @volatile private var asyncHookSet = false

  override def evaluateFuture[Result](handler: Handler[Result])(using
      ExecutionContext
  ) =
    val hook = new Thread(() =>
      handler(Event.Interrupt)
      close()
    )
    rt.addShutdownHook(hook)
    asyncHookSet = true

    val fut = Future(evaluate(handler))
    fut.onComplete: _ =>
      rt.removeShutdownHook(hook)
      asyncHookSet = false

    fut
  end evaluateFuture

  private var flags = Option.empty[CLong]

  private def changemode(rawMode: Boolean) =
    val state        = stackalloc[termios]()
    val STDIN_FILENO = 0
    if rawMode then
      tcgetattr(STDIN_FILENO, state)
      this.synchronized:
        flags = Some((!state)._4)
      (!state)._4 = (!state)._4 & ~(ICANON | ECHO)
      assert(tcsetattr(STDIN_FILENO, TCSANOW, state) == 0)
    else
      flags.foreach: oldflags =>
        tcgetattr(STDIN_FILENO, state)
        (!state)._4 = oldflags
        this.synchronized:
          flags = None
        assert(tcsetattr(STDIN_FILENO, TCSANOW, state) == 0)
    end if
  end changemode

  override def evaluate[Result](handler: Handler[Result]): Completion[Result] =
    changemode(rawMode = true)

    val hook = new Thread(() =>
      handler(Event.Interrupt)
      close()
    )
    if !asyncHookSet then rt.addShutdownHook(hook)

    var lastRead = 0

    inline def read() =
      lastRead = getchar()
      lastRead

    try
      boundary[Completion[Result]]:

        def whatNext(n: Next[Result]) =
          n match
            case Next.Continue    =>
            case Next.Done(value) => break(Completion.Finished(value))
            case Next.Stop        => break(Completion.interrupted)
            case Next.Error(msg)  => break(Completion.error(msg))

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

        Completion.interrupted
    finally if !asyncHookSet then rt.removeShutdownHook(hook)
    end try

  end evaluate

  override def close() = changemode(rawMode = false)
end InputProviderImpl
