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

private[cue4s] class InteractiveMultipleChoice(
    prompt: Prompt.MultipleChoice,
    terminal: Terminal,
    out: Output,
    colors: Boolean
):
  import InteractiveMultipleChoice.*
  private val preSelected   = prompt.alts.zipWithIndex.filter(_._1._2).map(_._2)
  private val altsWithIndex = prompt.alts.map(_._1).zipWithIndex
  private val initialState = State(
    text = "",
    selected = preSelected.toSet,
    showing = Some(altsWithIndex.map(_._2) -> 0),
    all = altsWithIndex,
    status = Status.Running
  )

  private var state = Transition(
    initialState
  )
  private var rendering       = Transition(renderState(state.current))
  private lazy val altMapping = altsWithIndex.map(_.swap).toMap

  private def colored(msg: String)(f: String => fansi.Str) =
    if colors then f(msg).toString else msg

  private def renderState(st: State): List[String] =
    val lines = List.newBuilder[String]

    extension (t: String)
      def bold =
        colored(t)(fansi.Bold.On(_))
      def green =
        colored(t)(fansi.Color.Green(_))
      def cyan =
        colored(t)(fansi.Color.Cyan(_))
      def underline =
        colored(t)(fansi.Underlined.On(_))
    end extension

    st.status match
      case Status.Running =>
        lines += colored(prompt.lab + " > ")(fansi.Color.Cyan(_)) + st.text
        lines += "Tab".bold + " to toggle, " + "Enter".bold + " to submit."

        st.showing match
          case None =>
            lines += colored("no matches...")(fansi.Bold.On(_))
          case Some((filtered, selected)) =>
            filtered.foreach: id =>
              val alt = altMapping(id)
              if st.selected(id) then
                if id == selected then lines += s" ✔ " + alt.underline.green
                else lines += s" ✔ " + alt.underline
              else
                lines.addOne(
                  if id == selected then s" ‣ $alt".green
                  else s"   $alt"
                )
        end match

      case Status.Finished(ids) =>
        lines += "✔ ".green + prompt.lab.cyan

        if ids.isEmpty then lines += "nothing selected".underline
        else
          ids.toList.sorted
            .map(altMapping)
            .foreach: value =>
              lines += s" ‣ " + value.bold

      case Status.Canceled =>
        lines += colored("× ")(fansi.Color.Red(_)) +
          colored(prompt.lab + " ")(fansi.Color.Cyan(_))
        lines += ""
    end match

    lines.result()
  end renderState

  val handler = new Handler[List[String]]:
    def apply(event: Event): Next[List[String]] =
      event match
        case Event.Init =>
          terminal.cursorHide()
          printPrompt()
          Next.Continue

        case Event.Key(KeyEvent.UP) =>
          stateTransition(_.up)
          printPrompt()
          Next.Continue

        case Event.Key(KeyEvent.DOWN) =>
          stateTransition(_.down)
          printPrompt()
          Next.Continue

        case Event.Key(KeyEvent.ENTER) =>
          stateTransition(_.finish)
          state.current.status match
            case Status.Canceled => Next.Stop
            case Status.Running  => Next.Continue
            case Status.Finished(ids) =>
              val stringValues = ids.toList.sorted.map(altMapping.apply)
              printPrompt()
              terminal.cursorShow()
              Next.Done(stringValues)

        case Event.Key(KeyEvent.TAB) =>
          stateTransition(_.toggle)
          printPrompt()
          Next.Continue

        case Event.Key(KeyEvent.DELETE) =>
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

    private def printPrompt() =
      import terminal.*
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

end InteractiveMultipleChoice

object InteractiveMultipleChoice:
  enum Status:
    case Running
    case Finished(ids: Set[Int])
    case Canceled

  case class State(
      text: String,
      selected: Set[Int],
      showing: Option[(List[Int], Int)],
      all: List[(String, Int)],
      status: Status
  ):
    def up   = changeSelection(-1)
    def down = changeSelection(+1)

    def finish = copy(status = Status.Finished(selected))

    def cancel = copy(status = Status.Canceled)

    def addText(t: Char) =
      changeText(text + t)

    end addText

    def trimText =
      changeText(text.dropRight(1))

    def toggle =
      showing match
        case None => this
        case Some((_, cursor)) =>
          if selected(cursor) then copy(selected = selected - cursor)
          else copy(selected = selected + cursor)

    private def changeText(newText: String) =
      val newFiltered = all.filter((alt, _) =>
        newText.trim.isEmpty || alt
          .toLowerCase()
          .contains(newText.toLowerCase().trim())
      )
      if newFiltered.nonEmpty then
        showing match
          case None =>
            val newShowing = newFiltered.headOption.map: (_, id) =>
              newFiltered.map(_._2) -> id

            copy(text = newText, showing = newShowing)

          case Some((_, selected)) =>
            val newShowing = newFiltered.headOption.map: (_, id) =>
              val newSelected =
                newFiltered.find(_._2 == selected).map(_._2).getOrElse(id)
              newFiltered.map(_._2) -> newSelected

            copy(text = newText, showing = newShowing)
      else copy(showing = None, text = newText)
      end if
    end changeText

    private def changeSelection(move: Int) =
      showing match
        case None => this // do nothing, no alternatives are showing
        case a @ Some((filtered, showing)) =>
          val position = filtered.indexOf(showing)

          val newSelected =
            (position + move).max(0).min(filtered.length - 1)

          copy(showing = a.map(_ => (filtered, filtered(newSelected))))

  end State
end InteractiveMultipleChoice
