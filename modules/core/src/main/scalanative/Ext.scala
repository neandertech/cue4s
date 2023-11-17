package com.indoorvivants.proompts

import scalanative.libc.stdio.getchar
import scalanative.unsafe.*
import scalanative.posix.termios.*
import scala.util.boundary, boundary.break

import CharCollector.*

class NativeInputProvider extends InputProvider:
  override def attach(f: Environment => Handler): Completion =
    changemode(1)

    var lastRead = 0

    inline def read() =
      lastRead = getchar()
      lastRead

    val env = Environment(str =>
      System.err.println(str.getBytes().toList)
      System.out.write(str.getBytes())
      System.out.flush()
    )

    val listener = f(env)

    boundary[Completion]:

      def whatNext(n: Next) =
        n match
          case Next.Continue   =>
          case Next.Stop       => break(Completion.Interrupted)
          case Next.Error(msg) => break(Completion.Error(msg))

      def send(ev: Event) =
        whatNext(listener(ev))

      var state = State.Init

      whatNext(listener(Event.Init))

      while read() != 0 do

        val (newState, result) = decode(state, lastRead)

        result match
          case n: Next => whatNext(n)
          case e: Event =>
            send(e)

        state = newState

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
