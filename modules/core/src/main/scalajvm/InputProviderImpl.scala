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
import scala.concurrent.Promise
import scala.util.Success
import scala.util.boundary

import boundary.break
import CharCollector.*
import scala.concurrent.Await
import scala.concurrent.duration.Duration

private class InputProviderImpl(o: Terminal)
    extends InputProvider(o),
      InputProviderPlatform:

  @volatile private var asyncHookSet = false

  override def evaluateFuture[Result](handler: Handler[Result])(using
      ExecutionContext,
  ) =

    val cancellation = Promise[Completion[Result]]()

    val hook = () =>
      handler(TerminalEvent.Interrupt)
      o.cursorShow()
      cancellation.complete(Success(Completion.interrupted))
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

  override def evaluate[Result](handler: Handler[Result]): Completion[Result] =
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

    if !asyncHookSet then hook.foreach(InputProviderImpl.addShutdownHook)

    def send(ev: TerminalEvent): Boolean =
      whatNext(handler(ev))

    val readingThread = KeyboardReadingThread(
      whatNext,
      send,
      () => InputProviderImpl.nativeInterop.getchar(),
    )

    readingThread.start()

    whatNext(handler(TerminalEvent.Init))

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
      catch case e: Throwable => ()

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

end InputProviderImpl
