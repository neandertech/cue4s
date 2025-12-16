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
import scala.util.boundary

import CharCollector.*
import boundary.break

private class InputProviderImpl(o: Terminal)
    extends InputProvider(o),
      InputProviderPlatform:

  @volatile private var asyncHookSet = false

  val changeMode = ChangeModeNative.instance

  override def evaluateFuture[Result](handler: TerminalHandler[Result])(using
      ExecutionContext,
  ) =
    val hook = () =>
      handler(TerminalEvent.Interrupt)
      o.cursorShow()
      close()

    InputProviderImpl.addShutdownHook(hook)

    this.synchronized:
      asyncHookSet = true

    val fut = Future(evaluate(handler))
    fut.onComplete: _ =>
      InputProviderImpl.removeShutdownHook(hook)
      this.synchronized:
        asyncHookSet = false

    fut
  end evaluateFuture

  override def evaluate[Result](
      handler: TerminalHandler[Result],
  ): Completion[Result] =
    changeMode.changeMode(rawMode = true)

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

    val hook = () =>
      whatNext(handler(TerminalEvent.Interrupt))
      o.cursorShow()
      close()

    if !asyncHookSet then InputProviderImpl.addShutdownHook(hook)

    handler.setupBackchannel(whatNext(_))

    def send(ev: TerminalEvent): Boolean =
      whatNext(handler(ev))

    val readingThread = KeyboardReadingThread(
      whatNext,
      send,
      () => changeMode.read(),
    )

    readingThread.start()

    whatNext(handler(TerminalEvent.Init))

    result.future.onComplete: _ =>
      readingThread.interrupt()

    given ExecutionContext = ExecutionContext.global

    val completed = Await.result(result.future, Duration.Inf)
    if !asyncHookSet then InputProviderImpl.removeShutdownHook(hook)

    readingThread.join()

    completed

  end evaluate

  override def close() = changeMode.changeMode(rawMode = false)
end InputProviderImpl

private object InputProviderImpl:
  import scala.collection.mutable

  private val rt                             = Runtime.getRuntime()
  private val hooks: mutable.Set[() => Unit] = mutable.Set.empty

  def addShutdownHook(f: () => Unit): Unit =
    InputProviderImpl.synchronized:
      hooks.add(f)

  def removeShutdownHook(f: () => Unit): Unit =
    InputProviderImpl.synchronized:
      hooks.remove(f)
end InputProviderImpl
