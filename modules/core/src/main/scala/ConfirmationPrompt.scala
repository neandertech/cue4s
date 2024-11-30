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

private[cue4s] class ConfirmationPrompt(
    prompt: String,
    default: Boolean,
    terminal: Terminal,
    out: Output,
    theme: Theme,
) extends PromptFramework[Boolean](terminal, out):
  import ConfirmationPrompt.*

  override type PromptState = State

  override def initialState: PromptState = State.Running

  override def handleEvent(event: Event): PromptAction[Boolean] =
    event match
      case Event.Init => PromptAction.Start
      case Event.Key(KeyEvent.ENTER) =>
        PromptAction.Submit(result => _ => State.Finished(default))
      case Event.Char(which) =>
        which match
          case 'y' | 'Y' =>
            PromptAction.Submit(result => _ => State.Finished(true))
          case 'n' | 'N' =>
            PromptAction.Submit(result => _ => State.Finished(false))
          case _ => PromptAction.Continue
      case Event.Interrupt =>
        PromptAction.UpdateAndStop(_ => State.Canceled)
      case _ =>
        PromptAction.Continue
  end handleEvent

  import theme.*

  override def renderState(
      state: State,
      error: Option[PromptError],
  ): List[String] =
    val lines = List.newBuilder[String]

    state match
      case State.Running =>
        lines += "? ".selected + prompt.prompt +
          (" › (" + (if default then "Y/n" else "y/N") + ")").hint
        error.foreach: err =>
          lines += err.error
        lines += ""
      case State.Finished(res) =>
        lines += "✔ ".selected + prompt.emphasis + " … ".hint +
          (if res then "yes" else "no")
        lines += ""
      case State.Canceled =>
        lines += "× ".canceled + prompt.emphasis
        lines += ""
    end match

    lines.result()
  end renderState

  override def isRunning(state: State): Boolean = state == State.Running

  override def result(state: State): Either[PromptError, Boolean] = state match
    case State.Running          => Right(false)
    case State.Finished(result) => Right(result)
    case State.Canceled         => Left(PromptError("cancelled"))

end ConfirmationPrompt

private[cue4s] object ConfirmationPrompt:
  enum State:
    case Running
    case Finished(result: Boolean)
    case Canceled
