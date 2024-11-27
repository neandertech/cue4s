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

package cue4s.catseffect

import cats.effect.*
import cue4s.*

class PromptsIO private (
    protected val out: Output,
    protected val terminal: Terminal,
    protected val colors: Boolean
) extends AutoCloseable:
  protected lazy val inputProvider = InputProvider(out)

  def io[A](
      prompt: Prompt[A]
  ): IO[Completion[A]] =
    val inputProvider = InputProvider(out)
    val handler       = prompt.handler(terminal, out, colors)

    // TODO: provide native CE interface here
    IO.executionContext
      .flatMap: ec =>
        IO.fromFuture(IO(inputProvider.evaluateFuture(handler)(using ec)))
      .guarantee(IO(terminal.cursorShow()))
      .guarantee(IO(inputProvider.close()))

  end io

  override def close(): Unit = inputProvider.close()
end PromptsIO

object PromptsIO:
  def apply(
      out: Output = Output.Std,
      createTerminal: Output => Terminal = Terminal.ansi,
      colors: Boolean = true
  ): Resource[IO, PromptsIO] = Resource.fromAutoCloseable(
    IO(new PromptsIO(out, createTerminal(out), colors))
  )
end PromptsIO
