package cue4s

import cue4s.CharCollector.*

private[cue4s] class KeyboardReadingThread[Result](
    whatNext: Next[Result] => Boolean,
    send: TerminalEvent => Boolean,
    getchar: () => Int
) extends Thread:
  override def run(): Unit =
    var lastRead = 0

    inline def read() =
      lastRead = getchar()
      lastRead

    var state = State.Init
    var stop  = false

    while !stop && read() != 0 do
      val (newState, result) = decode(state, lastRead)

      result match
        case n: DecodeResult  => stop = whatNext(n.toNext)
        case e: TerminalEvent => stop = send(e)

      state = newState

    end while
  end run
end KeyboardReadingThread
