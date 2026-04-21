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
    hideText: Boolean,
    symbols: Symbols,
    default: Option[String],
) extends PromptFramework[String](terminal, out):

  import InteractiveTextInput.*

  override type PromptState = State
  override type Event       = TerminalEvent

  override def initialState: State = State(default.getOrElse(""), None)

  override def extractResult(state: State): Either[PromptError, String] =
    Right(state.text)

  override def handleEvent(event: TerminalEvent) =

    event match
      case TerminalEvent.Key(KeyEvent.ENTER) =>
        PromptAction.TrySubmit

      case TerminalEvent.Key(KeyEvent.DELETE) =>
        PromptAction.updateState(_.trimText)

      case TerminalEvent.Char(which) =>
        PromptAction.updateState(_.addText(which.toChar))

      case TerminalEvent.Interrupt => PromptAction.Stop

      case TerminalEvent.Resized(rows, cols) =>
        PromptAction.updateState(_.copy(terminalSize = Some(rows -> cols)))

      case _ => PromptAction.Continue
    end match
  end handleEvent

  import theme.*

  override def renderState(
      st: State,
      status: Status,
  ): List[String] =
    val lines = List.newBuilder[fansi.Str]
    import symbols.*

    val txt = if hideText then "*" * st.text.length else st.text

    status match
      case Status.Running(err) =>
        lines += "? ".focused ++ prompt.prompt ++ s" $promptCue ".hint ++ txt.input
        err.left.toOption.foreach: err =>
          lines += err.error
      case Status.Finished(res) =>
        val txt = if hideText then "*" * res.length else res
        lines += s"$promptDone ".focused ++ prompt.prompt ++ s" $ellipsis ".hint ++ txt.emphasis
      case Status.Canceled =>
        lines += s"$promptCancelled ".canceled ++ prompt.prompt
    end match
    // lines += ""

    lines.result().map(_.render)

  end renderState

end InteractiveTextInput

private[cue4s] object InteractiveTextInput:
  case class State(
      text: String,
      terminalSize: Option[(TerminalRows, TerminalCols)],
  ):
    def addText(t: Char) = copy(text = text + t)

    def trimText = copy(text = text.dropRight(1))
  end State
end InteractiveTextInput
