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
):
  import InteractiveTextInput.*
  private var state     = Transition(State("", prompt.validate, Status.Running))
  private var rendering = Transition(renderState(state.current))

  private def renderState(st: State): List[String] =
    val lines = List.newBuilder[String]

    extension (t: String)
      def bold =
        colored(t)(fansi.Bold.On(_))
      def green =
        colored(t)(fansi.Color.Green(_))
      def cyan =
        colored(t)(fansi.Color.Cyan(_))
      def red =
        colored(t)(fansi.Color.Red(_))
    end extension

    st.status match
      case Status.Running =>
        lines += prompt.lab.cyan + " > " + state.current.text.bold
        st.error.foreach: err =>
          lines += err.red
      case Status.Finished(result) =>
        lines += "✔ ".green + prompt.lab.cyan + " " + state.current.text.bold
      case Status.Canceled =>
        lines += "× ".red + prompt.lab.cyan
    end match

    lines.result()
  end renderState

  private def colored(msg: String)(f: String => fansi.Str) =
    if colors then f(msg).toString else msg

  private def printPrompt() =
    import terminal.*
    cursorHide()
    rendering.last match
      case None =>
        // initial print
        rendering.current.foreach(out.outLn)
        moveUp(rendering.current.length).moveHorizontalTo(0)
      case Some(value) =>
        def render =
          rendering.current
            .zip(value)
            .foreach: (line, oldLine) =>
              if line != oldLine then
                moveHorizontalTo(0).eraseEntireLine()
                out.out(line)
              moveDown(1)

        if state.current.status == Status.Running then
          render
          moveUp(rendering.current.length).moveHorizontalTo(0)
        else // we are finished
          render
          // do not leave empty lines behind - move cursor up
          moveUp(rendering.current.reverse.takeWhile(_.isEmpty()).length)
    end match

  end printPrompt

  val handler = new Handler[String]:
    def apply(event: Event): Next[String] =
      event match
        case Event.Init =>
          printPrompt()
          Next.Continue

        case Event.Key(KeyEvent.ENTER) => // enter
          stateTransition(_.finish)
          state.current.status match
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
    end apply

  private def stateTransition(s: State => State) =
    state = state.nextFn(s)
    rendering = rendering.nextFn: currentRendering =>
      val newRendering = renderState(state.current)
      if newRendering.length < currentRendering.length then
        newRendering ++ List.fill(
          currentRendering.length - newRendering.length
        )("")
      else newRendering
  end stateTransition

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
