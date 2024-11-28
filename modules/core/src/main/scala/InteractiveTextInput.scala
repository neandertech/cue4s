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
    colors: Boolean,
    validate: String => Option[PromptError],
) extends PromptFramework[String](terminal, out):

  import InteractiveTextInput.*

  override type PromptState = State

  override def initialState: State              = State("", Status.Running)
  override def isRunning(state: State): Boolean = state.status == Status.Running

  override def result(state: State) =
    validate(state.text).toLeft(state.text)

  override def handleEvent(event: Event) =
    event match
      case Event.Init => PromptAction.Start

      case Event.Key(KeyEvent.ENTER) =>
        PromptAction.Submit(result => state => state.finish(result))

      case Event.Key(KeyEvent.DELETE) =>
        PromptAction.Update(_.trimText)

      case Event.Char(which) => PromptAction.Update(_.addText(which.toChar))

      case Event.Interrupt => PromptAction.UpdateAndStop(_.cancel)

      case _ => PromptAction.Continue
    end match
  end handleEvent

  private lazy val formatting = TextFormatting(colors)
  import formatting.*

  override def renderState(
      st: State,
      error: Option[PromptError],
  ): List[String] =
    val lines = List.newBuilder[String]

    st.status match
      case Status.Running =>
        lines += prompt.cyan + " > " + st.text.bold
        error.foreach: err =>
          lines += err.red
      case Status.Finished(res) =>
        lines += "✔ ".green + prompt.cyan + " " + st.text.bold
        lines += ""
      case Status.Canceled =>
        lines += "× ".red + prompt.cyan
        lines += ""
    end match

    lines.result()
  end renderState

end InteractiveTextInput

private[cue4s] object InteractiveTextInput:
  enum Status:
    case Running
    case Finished(res: String)
    case Canceled

  case class State(
      text: String,
      status: Status,
  ):
    def cancel                 = copy(status = Status.Canceled)
    def finish(result: String) = copy(status = Status.Finished(result))
    def addText(t: Char)       = copy(text = text + t)

    def trimText = copy(text = text.dropRight(1))
  end State
end InteractiveTextInput
