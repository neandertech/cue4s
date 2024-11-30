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

private[cue4s] class InteractiveTextInput(
    prompt: String,
    terminal: Terminal,
    out: Output,
    theme: Theme,
    validate: String => Option[PromptError],
) extends PromptFramework[String](terminal, out):

  import InteractiveTextInput.*

  override type PromptState = State

  override def initialState: State = State("")

  override def handleEvent(event: Event) =
    def update(state: State, f: State => State) =
      val newState = f(state)
      val newStatus =
        Status.Running(validate(newState.text).toLeft(newState.text))

      PromptAction.Update(_ => newStatus, _ => newState)

    out.logLn(s"handling $event")

    event match
      case Event.Key(KeyEvent.ENTER) =>
        currentStatus() match
          case Status.Running(Right(candidate)) =>
            PromptAction.updateStatus(_ => Status.Finished(candidate))
          case _ => PromptAction.Continue

      case Event.Key(KeyEvent.DELETE) =>
        update(currentState(), _.trimText)

      case Event.Char(which) => update(currentState(), _.addText(which.toChar))

      case Event.Interrupt => PromptAction.updateStatus(_ => Status.Canceled)

      case _ => PromptAction.Continue
    end match
  end handleEvent

  import theme.*

  override def renderState(
      st: State,
      status: Status,
  ): List[String] =
    val lines = List.newBuilder[String]

    status match
      case Status.Init =>
        lines += prompt.prompt + " > " + st.text.input
      case Status.Running(err) =>
        lines += prompt.prompt + " > " + st.text.input
        err.left.toOption.foreach: err =>
          lines += err.error
      case Status.Finished(res) =>
        lines += "✔ ".selected + prompt.prompt + " " + st.text.emphasis
        lines += ""
      case Status.Canceled =>
        lines += "× ".canceled + prompt.prompt
        lines += ""
    end match

    lines.result()
  end renderState

end InteractiveTextInput

private[cue4s] object InteractiveTextInput:
  case class State(
      text: String,
  ):
    def addText(t: Char) = copy(text = text + t)

    def trimText = copy(text = text.dropRight(1))
  end State
end InteractiveTextInput
