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

package com.indoorvivants.proompts

import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.annotation.JSImport

import com.indoorvivants.proompts.CharCollector.State
import com.indoorvivants.proompts.CharCollector.decode

import scalajs.js

trait InputProviderPlatform:
  def apply(): InputProvider = new InputProvider:
    override def attach(
        f: Environment => Handler
    ): Completion =

      val stdin = Process.stdin
      if stdin.isTTY.contains(true) then

        stdin.setRawMode(true)
        val env = Environment(writer = s =>
          System.err.println(s.getBytes.toList)
          Process.stdout.write(s)
        )

        val handler = f(env)

        val rl = Readline.createInterface(
          js.Dynamic.literal(
            input = stdin,
            escapeCodeTimeout = 50
          )
        )

        Readline.emitKeypressEvents(stdin, rl)

        var state = State.Init

        var completion = Completion.Finished

        lazy val keypress: js.Function = (str: js.UndefOr[String], key: Key) =>
          handle(key)

        def close(res: Completion) =
          stdin.setRawMode(false)
          rl.close()
          stdin.removeListener("keypress", keypress)
          completion = res

        def whatNext(n: Next) =
          n match
            case Next.Continue   =>
            case Next.Stop       => close(Completion.Interrupted)
            case Next.Error(msg) => close(Completion.Error(msg))

        def send(ev: Event) =
          whatNext(handler(ev))

        def handle(key: Key) =
          if key.name == "c" && key.ctrl then
            stdin.setRawMode(false)
            rl.close()
            stdin.removeListener("keypress", keypress)
          else
            key.sequence
              .getBytes()
              .foreach: byte =>
                val (newState, result) = decode(state, byte)

                state = newState

                result match
                  case n: Next  => whatNext(n)
                  case e: Event => send(e)

        handler(Event.Init)
        stdin.on("keypress", keypress)

        completion
      else Completion.Error("STDIN is not a TTY")
      end if
    end attach
    override def close(): Unit = ()
end InputProviderPlatform
