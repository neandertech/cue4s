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
    prompt: Prompt.Input,
    terminal: Terminal,
    out: Output,
    colors: Boolean
) extends PromptFramework(terminal, out):

  import InteractiveTextInput.*

  override type PromptState = State
  override type Result      = String

  override def initialState: State = State("", prompt.validate, Status.Running)
  override def isRunning(state: State): Boolean = state.status == Status.Running

  override def handleEvent(event: Event): Next[Result] =
    event match
      case Event.Init =>
        printPrompt()
        Next.Continue

      case Event.Key(KeyEvent.ENTER) => // enter
        stateTransition(_.finish)
        currentState().status match
          case Status.Running => Next.Continue
          case Status.Finished(result) =>
            printPrompt()
            terminal.cursorShow()
            Next.Done(result)
          case Status.Canceled =>
            Next.Stop

      case Event.Key(KeyEvent.DELETE) => // enter
        stateTransition(_.trimText)
        printPrompt()
        Next.Continue

      case Event.Char(which) =>
        stateTransition(_.addText(which.toChar))
        printPrompt()
        Next.Continue

      case Event.Interrupt =>
        stateTransition(_.cancel)
        printPrompt()
        terminal.cursorShow()
        Next.Stop

      case _ =>
        Next.Continue
    end match
  end handleEvent

  private lazy val formatting = TextFormatting(colors)
  import formatting.*

  override def renderState(st: State): List[String] =
    val lines = List.newBuilder[String]

    st.status match
      case Status.Running =>
        lines += prompt.lab.cyan + " > " + st.text.bold
        st.error.foreach: err =>
          lines += err.red
      case Status.Finished(result) =>
        lines += "✔ ".green + prompt.lab.cyan + " " + st.text.bold
        lines += ""
      case Status.Canceled =>
        lines += "× ".red + prompt.lab.cyan
        lines += ""
    end match

    lines.result()
  end renderState

end InteractiveTextInput

private[cue4s] object InteractiveTextInput:
  enum Status:
    case Running
    case Finished(result: String)
    case Canceled

  case class State(
      text: String,
      validate: String => Option[PromptError],
      status: Status
  ):
    lazy val error = validate(text)

    def cancel = copy(status = Status.Canceled)

    def finish =
      error match
        case None        => copy(status = Status.Finished(text))
        case Some(value) => this

    def addText(t: Char) = copy(text = text + t)
    def trimText         = copy(text = text.dropRight(1))
  end State
end InteractiveTextInput
