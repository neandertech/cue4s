/*
 * Copyright 2023 Anton Sviridov
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

package proompts

class InteractiveTextInput(
    prompt: Prompt.Input,
    terminal: Terminal,
    out: Output,
    colors: Boolean
):
  val lab   = prompt.lab + " > "
  var state = TextInputState("")

  def colored(msg: String)(f: String => fansi.Str) =
    if colors then f(msg).toString else msg

  def printPrompt() =

    import terminal.*

    moveHorizontalTo(0)
    eraseToEndOfLine()

    out.out(colored(lab + state.text)(fansi.Color.Cyan(_)))
  end printPrompt

  def printFinished() =
    terminal.moveHorizontalTo(0).eraseToEndOfLine()
    out.out(colored("✔ ")(fansi.Color.Green(_)))
    out.out(colored(prompt.lab + ": " + state.text + "\n")(fansi.Color.Cyan(_)))

  val handler = new Handler[String]:
    def apply(event: Event): Next[String] =
      event match
        case Event.Init =>
          printPrompt()
          Next.Continue

        case Event.Key(KeyEvent.ENTER) => // enter
          printFinished()
          Next.Done(state.text)

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
    state = state.copy(text = state.text + t)

  def trimText() =
    state = state.copy(text = state.text.take(state.text.length - 1))
end InteractiveTextInput
