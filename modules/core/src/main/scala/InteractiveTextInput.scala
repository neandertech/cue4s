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
  val lab   = prompt.lab + " > "
  var state = Transition(InteractiveTextInput.State("", prompt.validate))

  def colored(msg: String)(f: String => fansi.Str) =
    if colors then f(msg).toString else msg

  def printPrompt() =

    import terminal.*

    moveHorizontalTo(0)
    eraseToEndOfLine()

    if state.current.error.isEmpty && state.last.flatMap(_.error).nonEmpty then
      withRestore:
        moveDown(1)
        eraseToEndOfLine()

    out.out(colored(lab)(fansi.Color.Cyan(_)))
    out.out(colored(state.current.text)(fansi.Bold.On(_)))
    state.current.error match
      case None =>
      case Some(value) =>
        withRestore:
          out.out("\n")
          out.out(colored(value.toString())(fansi.Color.Red(_)))

  end printPrompt

  def printFinished() =
    import terminal.*
    moveHorizontalTo(0)
    eraseToEndOfLine()

    if state.last.flatMap(_.error).nonEmpty then
      withRestore:
        moveDown(1)
        eraseToEndOfLine()

    out.out(colored("✔ ")(fansi.Color.Green(_)))
    out.out(colored(prompt.lab + " ")(fansi.Color.Cyan(_)))
    out.out(colored(state.current.text + "\n")(fansi.Bold.On(_)))
  end printFinished

  val handler = new Handler[String]:
    def apply(event: Event): Next[String] =
      event match
        case Event.Init =>
          printPrompt()
          Next.Continue

        case Event.Key(KeyEvent.ENTER) => // enter
          if state.current.error.isEmpty then
            printFinished()
            Next.Done(state.current.text)
          else Next.Continue

        case Event.Key(KeyEvent.DELETE) => // enter
          trimText()
          printPrompt()
          Next.Continue

        case Event.Char(which) =>
          appendText(which.toChar)
          printPrompt()
          Next.Continue

        case _ =>
          Next.Continue
      end match
    end apply

  def appendText(t: Char) =
    state = state.nextFn(r => r.copy(text = r.text + t))

  def trimText() =
    state = state.nextFn(r => r.copy(text = r.text.take(r.text.length - 1)))
end InteractiveTextInput

private[cue4s] object InteractiveTextInput:
  case class State(text: String, validate: String => Option[PromptError]):
    lazy val error = validate(text)

    override def toString(): String = s"State[text=`$text`, error=`$error`]"
