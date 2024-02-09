package com.indoorvivants.proompts

import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.annotation.JSImport

import com.indoorvivants.proompts.CharCollector.State
import com.indoorvivants.proompts.CharCollector.decode

import scalajs.js
import scala.concurrent.Future

private class InputProviderImpl(o: Output)
    extends InputProvider(o),
      InputProviderPlatform:
  override def evaluateFuture(
      f: Interactive
  ): Future[Completion] =

    val stdin = Process.stdin
    if stdin.isTTY.contains(true) then

      stdin.setRawMode(true)
      // val env = Environment(writer = s =>
      //   System.err.println(s.getBytes.toList)
      //   Process.stdout.write(s)
      // )

      val handler = f.handler

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

      Future.successful(completion) // TODO fix
    else Future.successful(Completion.Error("STDIN is not a TTY"))
    end if
  end evaluateFuture
  override def close(): Unit = ()
end InputProviderImpl
