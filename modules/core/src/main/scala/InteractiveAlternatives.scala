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

class InteractiveAlternatives(
    prompt: AlternativesPrompt,
    terminal: Terminal,
    out: Output,
    colors: Boolean
):
  val lab           = prompt.lab + " > "
  val altsWithIndex = prompt.alts.zipWithIndex
  var state         = AlternativesState("", Some(0), altsWithIndex)

  def colored(msg: String)(f: String => fansi.Str) =
    if colors then f(msg).toString else msg

  def clear(oldState: AlternativesState, newState: AlternativesState) =
    import terminal.*
    for _ <- 0 until state.showing.length - newState.showing.length do
      moveNextLine(1)
      moveHorizontalTo(1)
      eraseToEndOfLine()

  def printPrompt() =

    import terminal.*

    moveHorizontalTo(0)
    eraseEntireLine()

    out.out("· ")
    out.out(colored(lab + state.text)(fansi.Color.Cyan(_)))

    withRestore:
      out.out("\n")

      val filteredAlts =
        altsWithIndex.filter: (txt, _) =>
          state.text.isEmpty() || txt
            .toLowerCase()
            .contains(
              state.text.toLowerCase()
            )

      if filteredAlts.isEmpty then
        moveHorizontalTo(0)
        eraseToEndOfLine()
        out.out(colored("  no matches")(fansi.Underlined.On(_)))
        val newState = AlternativesState(
          state.text,
          selected = None,
          showing = Nil
        )
        clear(state, newState)
        state = newState
      else
        filteredAlts.zipWithIndex.foreach:
          case ((alt, originalIdx), idx) =>
            moveHorizontalTo(0)
            eraseToEndOfLine()
            val view =
              if state.selected.contains(idx) then
                colored(s"  ‣ $alt")(fansi.Color.Green(_))
              else colored(s"    $alt")(fansi.Bold.On(_))
            out.out(view.toString)
            if idx != filteredAlts.length - 1 then out.out("\n")

        val newState = state.copy(
          showing = filteredAlts,
          selected =
            if state.showing == filteredAlts then state.selected
            else Some(0)
        )

        clear(state, newState)
        state = newState

      end if
  end printPrompt

  def printFinished(value: String) =
    terminal.eraseEntireLine()
    terminal.moveHorizontalTo(0)
    out.out(colored("✔ ")(fansi.Color.Green(_)))
    out.out(colored(lab)(fansi.Color.Cyan(_)))
    out.out(colored(value + "\n")(fansi.Bold.On(_)))

  end printFinished

  val handler = new Handler[String]:
    def apply(event: Event): Next[String] =
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
          state.selected match
            case None => Next.Continue
            case Some(value) =>
              terminal.withRestore:
                clear(state, state.copy(showing = Nil))
              val stringValue = state.showing(value)._1
              printFinished(stringValue)
              Next.Done(stringValue)

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

  def selectUp() = state =
    state.copy(selected = state.selected.map(s => (s - 1).max(0)))

  def selectDown() = state =
    state.copy(selected = state.selected.map(s => (s + 1).min(1000)))

  def appendText(t: Char) =
    state = state.copy(text = state.text + t)

  def trimText() =
    state = state.copy(text = state.text.take(state.text.length - 1))

end InteractiveAlternatives
