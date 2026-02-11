package cue4s

import scala.scalanative.unsafe.*
import scala.scalanative.libc.signal.*

import types.*
import scala.scalanative.unsigned.UShort
import scala.scalanative.meta.LinktimeInfo

@extern
private[cue4s] def scalanative_sigwinch(): Int = extern

@extern
private[cue4s] def scalanative_get_window_size(ws: Ptr[WinSize]): Int = extern

private[cue4s] object types:
  opaque type WinSize = CStruct2[UShort, UShort]
  object WinSize:
    given Tag[WinSize] =
      Tag.materializeCStruct2Tag[UShort, UShort]
    extension (ws: WinSize)
      inline def row: CUnsignedShort = ws._1
      inline def col: CUnsignedShort = ws._2
    end extension
  end WinSize
end types

object TerminalResizingHandler:
  // We are using this var here because we can't capture @send variable from dynamic scope inside the C function pointer.
  // All the terms used in the signal handler must come from static scopes (in this case it has a stable path, TerminalResizingHandler.sender)
  private var sender = Option.empty[TerminalEvent.Resized => Unit]
  def use[A](send: TerminalEvent.Resized => Unit)(
      f: => A,
  ): A =
    // resizing is not implemented on windows (yet)
    if LinktimeInfo.isWindows then f
    else
      // Immediately after this handler is invoked, we send the first measurement of the terminal.
      // Users should receive `TerminalEvent.Init` and then `TerminalEvent.Resized`
      val ws = stackalloc[WinSize]()
      scalanative_get_window_size(ws)
      send(
        TerminalEvent
          .Resized(
            (!ws).row.toInt.asInstanceOf[TerminalRows],
            (!ws).col.toInt.asInstanceOf[TerminalCols],
          ),
      )
      try
        sender = Some(send)
        signal(
          scalanative_sigwinch(),
          CFuncPtr1.fromScalaFunction: _ =>
            val ws = stackalloc[WinSize]()
            scalanative_get_window_size(ws)
            sender.foreach(
              _.apply(
                TerminalEvent
                  .Resized(
                    (!ws).row.toInt.asInstanceOf[TerminalRows],
                    (!ws).col.toInt.asInstanceOf[TerminalCols],
                  ),
              ),
            ),
        )
        f
      finally
        signal(scalanative_sigwinch(), SIG_DFL)
        sender = None
      end try
  end use
end TerminalResizingHandler
