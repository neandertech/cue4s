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

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.util.Success

import sun.misc.Signal

private class InputProviderImpl(o: Terminal)
    extends InputProvider(o),
      InputProviderPlatform:

  @volatile private var asyncHookSet = false

  override def evaluateFuture[Result](handler: TerminalHandler[Result])(using
      ExecutionContext,
  ) =

    val cancellation = Promise[Completion[Result]]()

    val hook = () =>
      handler(TerminalEvent.Interrupt)
      o.cursorShow()
      ()

    InputProviderImpl.addShutdownHook(hook)
    this.synchronized:
      asyncHookSet = true

    val fut = Future(evaluate(handler))
    fut.onComplete: _ =>
      InputProviderImpl.removeShutdownHook(hook)
      this.synchronized:
        asyncHookSet = false

    Future.firstCompletedOf(Seq(cancellation.future, fut))
  end evaluateFuture

  override def evaluate[Result](
      handler: TerminalHandler[Result],
  ): Completion[Result] =
    InputProviderImpl.nativeInterop.changemode(1)

    val result = Promise[Completion[Result]]()
    def whatNext(n: Next[Result]): Boolean =
      if !result.isCompleted then
        n match
          case Next.Continue => false
          case Next.Done(value) =>
            result.complete(Success(Completion.Finished(value)))
            true
          case Next.Stop =>
            result.complete(Success(Completion.interrupted))
            true
          case Next.Error(msg) =>
            result.complete(Success(Completion.error(msg)))
            true
      else true

    var hook = Option.empty[() => Unit]

    handler.setupBackchannel(whatNext(_))

    hook = Some: () =>
      o.cursorShow()
      whatNext(handler(TerminalEvent.Interrupt))
      InputProviderImpl.nativeInterop.changemode(0)

    if !asyncHookSet then hook.foreach(InputProviderImpl.addShutdownHook)

    def send(ev: TerminalEvent): Boolean =
      whatNext(handler(ev))

    val readingThread = KeyboardReadingThread(
      whatNext,
      send,
      () => InputProviderImpl.nativeInterop.getchar(),
    )

    val nativeWindowSize = InputProviderImpl.nativeWindowSize

    whatNext(handler(TerminalEvent.Init))

    nativeWindowSize
      .map(_.getWinSize())
      .foreach: ws =>
        whatNext(
          handler(
            TerminalEvent.Resized(
              ws.getRows().asInstanceOf[TerminalRows],
              ws.getCols().asInstanceOf[TerminalCols],
            ),
          ),
        )

    val resizeSignalHandler =
      nativeWindowSize.foreach: ws =>
        Signal.handle(
          new Signal("WINCH"),
          _ =>
            val winSize = ws.getWinSize()
            whatNext(
              handler(
                TerminalEvent.Resized(
                  winSize.getRows().asInstanceOf[TerminalRows],
                  winSize.getCols().asInstanceOf[TerminalCols],
                ),
              ),
            ),
        )

    readingThread.start()

    result.future.onComplete: _ =>
      readingThread.interrupt()

    given ExecutionContext = ExecutionContext.global

    val completed = Await.result(result.future, Duration.Inf)
    if !asyncHookSet then hook.foreach(InputProviderImpl.removeShutdownHook)

    readingThread.join()

    completed
  end evaluate

  override def close() =
    InputProviderImpl.nativeInterop.changemode(0)

end InputProviderImpl

private object InputProviderImpl:
  import scala.collection.mutable

  private val rt                             = Runtime.getRuntime()
  private val hooks: mutable.Set[() => Unit] = mutable.Set.empty

  def addShutdownHook(f: () => Unit): Unit =
    this.synchronized:
      hooks.add(f)

  def removeShutdownHook(f: () => Unit): Unit =
    this.synchronized:
      hooks.remove(f)

  rt.addShutdownHook(Thread(() =>
    hooks.foreach: hook =>
      try hook()
      catch case _: Throwable => ()

    nativeInterop.changemode(0),
  ))

  lazy private val nativeInterop: ChangeMode =
    import Platform.*
    os match
      case OS.MacOS   => ChangeMode.forDarwin()
      case OS.Linux   => ChangeMode.forLinux()
      case OS.Windows => ChangeMode.forWindows()
      case OS.Unknown =>
        sys.error(
          "Cue4s failed to detect the operating system, it is likely unsupported. Please raise an issue (or even a PR!) at https://github.com/neandertech/cue4s",
        )
    end match
  end nativeInterop

  lazy private val nativeWindowSize: Option[GetWindowSize] =
    import Platform.*
    os match
      case OS.MacOS => Some(GetWindowSize.forDarwin())
      case OS.Linux => Some(GetWindowSize.forLinux())
      case _        => None
    end match
  end nativeWindowSize

end InputProviderImpl
