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

package com.indoorvivants.proompts

class InteractiveAlternatives(
    terminal: Terminal,
    prompt: Prompt.Alternatives,
    writer: String => Unit,
    colors: Boolean
):
  val lab   = prompt.promptLabel
  var state = AlternativesState("", 0, prompt.alts.length)

  def colored(msg: String)(f: String => fansi.Str) =
    if colors then f(msg).toString else msg

  def printPrompt() =

    import terminal.*

    moveHorizontalTo(0)
    eraseToEndOfLine()

    writer(colored(lab + state.text)(fansi.Color.Cyan(_)))

    withRestore:
      writer("\n")

      val filteredAlts =
        prompt.alts.filter(
          state.text.isEmpty() || _.toLowerCase().contains(
            state.text.toLowerCase()
          )
        )

      val adjustedSelected =
        state.selected.min(filteredAlts.length - 1).max(0)

      val newState =
        AlternativesState(
          state.text,
          selected = adjustedSelected,
          showing = filteredAlts.length.max(1)
        )

      if filteredAlts.isEmpty then
        moveHorizontalTo(0)
        eraseToEndOfLine()
        writer(colored("no matches")(fansi.Underlined.On(_)))
      else
        filteredAlts.zipWithIndex.foreach: (alt, idx) =>
          moveHorizontalTo(0)
          eraseToEndOfLine()
          val view =
            if idx == adjustedSelected then
              colored(s"> $alt")(fansi.Color.Green(_))
            else colored(s"· $alt")(fansi.Bold.On(_))
          writer(view.toString)
          if idx != filteredAlts.length - 1 then writer("\n")
      end if

      for _ <- 0 until state.showing - newState.showing do
        moveNextLine(1)
        moveHorizontalTo(0)
        eraseToEndOfLine()
      state = newState
  end printPrompt

  def handler = new Handler:
    def apply(event: Event): Next =
      event match
        case Event.Init =>
          printPrompt()
          Next.Continue
        case Event.Key(KeyEvent.UP) =>
          selectUp()
          printPrompt()
          Next.Continue
        case Event.Key(KeyEvent.DOWN) =>
          selectDown()
          printPrompt()
          Next.Continue

        case Event.Key(KeyEvent.ENTER) => // enter
          Next.Stop

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

  def selectUp() = state = state.copy(selected = (state.selected - 1).max(0))

  def selectDown() = state =
    state.copy(selected = (state.selected + 1).min(1000))

  def appendText(t: Char) =
    state = state.copy(text = state.text + t)

  def trimText() =
    state = state.copy(text = state.text.take(state.text.length - 1))

end InteractiveAlternatives
