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

private[cue4s] class InteractiveConfirmation(
    prompt: String,
    default: Boolean,
    terminal: Terminal,
    out: Output,
    theme: Theme,
) extends PromptFramework[Boolean](terminal, out):

  override type PromptState = Unit

  override def initialState: PromptState = ()

  override def handleEvent(event: Event): PromptAction =
    event match
      case Event.Init => PromptAction.Update()
      case Event.Key(KeyEvent.ENTER) =>
        PromptAction.setStatus(Status.Finished(default))
      case Event.Char(which) =>
        which match
          case 'y' | 'Y' =>
            PromptAction.setStatus(Status.Finished(true))
          case 'n' | 'N' =>
            PromptAction.setStatus(Status.Finished(false))
          case _ => PromptAction.Continue
      case Event.Interrupt =>
        PromptAction.setStatus(Status.Canceled)
      case _ =>
        PromptAction.Continue
  end handleEvent

  import theme.*

  override def renderState(
      state: Unit,
      status: Status,
  ): List[String] =
    val lines = List.newBuilder[String]

    status match
      case Status.Init =>
        lines += "? ".selected + prompt.prompt +
          (" › (" + (if default then "Y/n" else "y/N") + ")").hint
      case Status.Running(error) =>
        lines += "? ".selected + prompt.prompt +
          (" › (" + (if default then "Y/n" else "y/N") + ")").hint
        error.left.toOption.foreach: err =>
          lines += err.error
      case Status.Finished(res) =>
        lines += "✔ ".selected + prompt.prompt + " … ".hint +
          (if res then "yes" else "no").emphasis
      case Status.Canceled =>
        lines += "× ".canceled + prompt.emphasis
    end match

    lines += ""

    lines.result()
  end renderState

end InteractiveConfirmation
