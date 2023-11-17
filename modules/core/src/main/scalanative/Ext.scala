package com.indoorvivants.proompts

import scalanative.libc.stdio.getchar
import scalanative.unsafe.*
import scalanative.posix.termios.*
import scala.util.boundary, boundary.break

import CharCollector.*

class NativeInputProvider extends InputProvider:
  override def attach(listener: Event => Next): Completion =
    changemode(1)

    var lastRead = 0

    inline def read() =
      lastRead = getchar()
      lastRead

    boundary[Completion]:

      def whatNext(n: Next) =
        n match
          case Next.Continue   =>
          case Next.Stop       => break(Completion.Interrupted)
          case Next.Error(msg) => break(Completion.Error(msg))

      def send(ev: Event) =
        whatNext(listener(ev))

      var state = State.Init

      while read() != 0 do

        val (newState, result) = decode(state, lastRead)

        result match
          case n: Next => whatNext(n)
          case e: Event =>
            send(e)

        state = newState

        // lastRead match
        //   case ANSI.ESC =>
        //     assert(
        //       read() == '[',
        //       s"Invalid character following ESC: `$lastRead`"
        //     )
        //     val key = read()

        //     key match
        //       case 'A' => send(Event.Key(KeyEvent.UP))
        //       case 'B' => send(Event.Key(KeyEvent.DOWN))
        //       case 'C' => send(Event.Key(KeyEvent.RIGHT))
        //       case 'D' => send(Event.Key(KeyEvent.LEFT))
        //   case other => send(Event.Char(other))
        // end match
      end while

      Completion.Finished

  end attach

  override def close() = changemode(0)
end NativeInputProvider

trait InputProviderPlatform:
  def apply(): InputProvider = NativeInputProvider()

def changemode(dir: Int) =
  val oldt         = stackalloc[termios]()
  val newt         = stackalloc[termios]()
  val STDIN_FILENO = 0
  if dir == 1 then
    tcgetattr(STDIN_FILENO, oldt)
    !newt = !oldt
    (!newt)._4 = (!newt)._4 & ~(ICANON | ECHO)
    tcsetattr(STDIN_FILENO, TCSANOW, newt)
  else tcsetattr(STDIN_FILENO, TCSANOW, oldt)
end changemode
