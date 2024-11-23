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

private[cue4s] class InteractiveSingleChoice(
    prompt: Prompt.SingleChoice,
    terminal: Terminal,
    out: Output,
    colors: Boolean
):
  case class State(
      text: String,
      selected: Option[Int],
      showing: List[(String, Int)]
  )

  val lab           = prompt.lab + " > "
  val altsWithIndex = prompt.alts.zipWithIndex
  var state         = Transition(State("", Some(0), altsWithIndex))

  def colored(msg: String)(f: String => fansi.Str) =
    if colors then f(msg).toString else msg

  def clear() =
    val showedPreviously = state.last.map(_.showing.length).getOrElse(0)
    for _ <- 0 until showedPreviously - state.current.showing.length do
      terminal.moveNextLine(1).moveHorizontalTo(1).eraseToEndOfLine()

  // state.last.foreach:
  // val

  // def clear(oldState: State, newState: State) =
  //   import terminal.*
  //   for _ <- 0 until state.showing.length - newState.showing.length do
  //     moveNextLine(1)
  //     moveHorizontalTo(1)
  //     eraseToEndOfLine()

  def printPrompt() =

    import terminal.*

    moveHorizontalTo(0)
    eraseEntireLine()
    cursorHide()

    out.out("· ")
    out.out(colored(lab + state.current.text)(fansi.Color.Cyan(_)))

    withRestore:
      out.out("\n")

      val filteredAlts =
        altsWithIndex.filter: (txt, _) =>
          state.current.text.isEmpty() || txt
            .toLowerCase()
            .contains(
              state.current.text.toLowerCase()
            )

      if filteredAlts.isEmpty then
        moveHorizontalTo(0)
        eraseToEndOfLine()
        out.out(colored("  no matches")(fansi.Underlined.On(_)))
        state = state.nextFn(st => st.copy(selected = None, showing = Nil))
        clear()
        // val newState = State(
        //   state.text,
        //   selected = None,
        //   showing = Nil
        // )
        // clear(state, newState)
        // state = newState
      else
        filteredAlts.zipWithIndex.foreach:
          case ((alt, originalIdx), idx) =>
            moveHorizontalTo(0)
            eraseToEndOfLine()
            val view =
              if state.current.selected.contains(idx) then
                colored(s"  ‣ $alt")(fansi.Color.Green(_))
              else colored(s"    $alt")(fansi.Bold.On(_))
            out.out(view.toString)
            if idx != filteredAlts.length - 1 then out.out("\n")

        // val newState = state.copy(
        //   showing = filteredAlts,
        //   selected =
        //     if state.showing == filteredAlts then state.selected
        //     else Some(0)
        // )

        // clear(state, newState)
        state = state.nextFn(st =>
          st.copy(
            showing = filteredAlts,
            selected =
              if st.showing == filteredAlts then st.selected
              else Some(0)
          )
        )

        clear()

      end if
  end printPrompt

  def printFinished(value: String) =
    terminal.eraseEntireLine()
    terminal.moveHorizontalTo(0)
    out.out(colored("✔ ")(fansi.Color.Green(_)))
    out.out(colored(prompt.lab + " ")(fansi.Color.Cyan(_)))
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
          state.current.selected match
            case None => Next.Continue
            case Some(value) =>
              val stringValue = state.current.showing(value)._1
              terminal.withRestore:
                state = state.nextFn(st => st.copy(showing = Nil))
                clear()
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

  def selectUp() =
    state = state.nextFn(st =>
      st.copy(selected = st.selected.map(s => (s - 1).max(0)))
    )

  def selectDown() =
    state = state.nextFn(st =>
      st.copy(selected = st.selected.map(s => (s + 1).min(1000)))
    )

  def appendText(t: Char) =
    state = state.nextFn(st => st.copy(text = st.text + t))

  def trimText() =
    state = state.nextFn(st => st.copy(text = st.text.take(st.text.length - 1)))
end InteractiveSingleChoice
