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

class InteractiveMultipleChoice(
    prompt: Prompt.MultipleChoice,
    terminal: Terminal,
    out: Output,
    colors: Boolean
):
  case class State(
      text: String,
      selected: List[Int],
      current: Option[Int],
      showing: List[(String, Int)]
  )

  val lab           = prompt.lab + " > "
  val altsWithIndex = prompt.alts.zipWithIndex
  var state         = State("", Nil, current = Some(0), altsWithIndex)

  def colored(msg: String)(f: String => fansi.Str) =
    if colors then f(msg).toString else msg

  def clear(oldShowing: Int, newShowing: Int) =
    import terminal.*
    for _ <- 0 until oldShowing - newShowing do
      moveNextLine(1)
      moveHorizontalTo(1)
      eraseToEndOfLine()

  def printPrompt() =

    import terminal.*

    withRestore:
      moveHorizontalTo(0)
      eraseEntireLine()
      out.out(colored(lab + state.text)(fansi.Color.Cyan(_)))
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
        val newState = state.copy(
          showing = Nil,
          current = Some(0)
        )
        clear(state.showing.length, newState.showing.length)
        state = newState
      else
        filteredAlts.zipWithIndex.foreach:
          case ((alt, originalIdx), idx) =>
            moveHorizontalTo(0)
            eraseToEndOfLine()
            val view =
              if state.selected.contains(idx) then
                if state.current.contains(idx) then
                  colored(s"  ‣ $alt")(s => fansi.Bold.On(fansi.Color.Green(s)))
                else colored(s"  ‣ $alt")(fansi.Color.Green(_))
              else if state.current.contains(idx) then
                colored(s"  ▹ $alt")(fansi.Bold.On(_))
              else s"  ▹ $alt"
            out.out(view.toString)
            if idx != filteredAlts.length - 1 then out.out("\n")

        val newState = state.copy(
          showing = filteredAlts
        )

        clear(state.showing.length, newState.showing.length)
        state = newState

      end if
  end printPrompt

  def printFinished(values: List[String]) =
    terminal.eraseEntireLine()
    terminal.moveHorizontalTo(0)
    out.out(colored("✔ ")(fansi.Color.Green(_)))
    out.out(colored(prompt.lab + ":")(fansi.Color.Cyan(_)))
    out.out("\n")
    terminal.withRestore:
      (0 until state.showing.length).foreach: _ =>
        terminal.moveHorizontalTo(0)
        terminal.eraseEntireLine()
        terminal.moveNextLine(1)

    values.foreach: value =>
      out.out(colored(s"  ‣ $value" + "\n")(fansi.Bold.On(_)))

  end printFinished

  val handler = new Handler[List[String]]:
    def apply(event: Event): Next[List[String]] =
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

        case Event.Key(KeyEvent.ENTER) =>
          val resolved = state.selected.map(prompt.alts.apply)
          printFinished(resolved)
          Next.Done(resolved)
        // state.selected match
        //   case None => Next.Continue
        //   case Some(value) =>
        //     terminal.withRestore:
        //       clear(state, state.copy(showing = Nil))
        //     val stringValue = state.showing(value)._1
        //     printFinished(stringValue)
        //     Next.Done(List(stringValue))

        case Event.Key(KeyEvent.TAB) =>
          toggle()
          printPrompt()
          Next.Continue

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

  def toggle() =
    state.current match
      case None =>
      case Some(value) =>
        state.showing.lift(value) match
          case None =>
          case Some((_, realIdx)) =>
            state = state.copy(selected =
              if state.selected.contains(realIdx) then
                state.selected.diff(Seq(realIdx))
              else state.selected :+ realIdx
            )
    end match
  end toggle

  def selectUp() = state =
    state.copy(current = state.current.map(s => (s - 1).max(0)))

  def selectDown() = state =
    state.copy(current = state.current.map(s => (s + 1).min(1000)))

  def appendText(t: Char) =
    state = state.copy(text = state.text + t, current = Some(0))

  def trimText() =
    state = state.copy(
      text = state.text.take(state.text.length - 1),
      current = Some(0)
    )
end InteractiveMultipleChoice
