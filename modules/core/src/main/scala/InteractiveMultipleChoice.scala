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
) extends PromptFramework(terminal, out):
  import InteractiveMultipleChoice.*

  override type PromptState = State
  override type Result      = List[String]

  private lazy val preSelected =
    prompt.alts.zipWithIndex.filter(_._1._2).map(_._2)
  private lazy val altsWithIndex = prompt.alts.map(_._1).zipWithIndex

  override def isRunning(state: State): Boolean = state.status == Status.Running

  override def initialState = State(
    text = "",
    selected = preSelected.toSet,
    showing = Some(altsWithIndex.map(_._2) -> 0),
    all = altsWithIndex,
    status = Status.Running,
    0,
    10
  )

  private lazy val altMapping = altsWithIndex.map(_.swap).toMap

  private lazy val formatting = TextFormatting(colors)
  import formatting.*

  override def renderState(st: State): List[String] =
    val lines = List.newBuilder[String]

    st.status match
      case Status.Running =>
        lines += prompt.lab + " > ".cyan + st.text
        lines += "Tab".bold + " to toggle, " + "Enter".bold + " to submit."

        st.showing match
          case None =>
            lines += "no matches...".bold
          case Some((filtered, selected)) =>
            st.visibleEntries(filtered)
              .foreach: id =>
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
        lines += "× ".red +
          (prompt.lab + " ").cyan
        lines += ""
    end match

    lines.result()
  end renderState

  override def handleEvent(event: Event): Next[List[String]] =
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
        currentState().status match
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
  end handleEvent

end InteractiveMultipleChoice

private[cue4s] object InteractiveMultipleChoice:
  enum Status:
    case Running
    case Finished(ids: Set[Int])
    case Canceled

  case class State(
      text: String,
      selected: Set[Int],
      showing: Option[(List[Int], Int)],
      all: List[(String, Int)],
      status: Status,
      windowStart: Int,
      windowSize: Int
  ) extends InfiniscrollableState[State]:

    def finish = copy(status = Status.Finished(selected))

    def cancel = copy(status = Status.Canceled)

    override protected def scrollUp =
      copy(windowStart = (windowStart - 1).max(0))

    override protected def scrollDown = copy(windowStart = windowStart + 1)

    def addText(t: Char) =
      changeText(text + t).resetWindow()

    def trimText =
      changeText(text.dropRight(1)).resetWindow()

    def toggle =
      showing match
        case None => this
        case Some((_, cursor)) =>
          if selected(cursor) then copy(selected = selected - cursor)
          else copy(selected = selected + cursor)

    override protected def resetWindow() =
      copy(windowStart = 0)

    private def changeText(newText: String) =
      val newFiltered = all.filter((alt, _) =>
        newText.trim.isEmpty || alt
          .toLowerCase()
          .contains(newText.toLowerCase().trim())
      )
      if newFiltered.nonEmpty then
        val newShowing = Some(
          newFiltered.map(_._2) -> newFiltered.head._2
        )
        copy(text = newText, showing = newShowing)
      else copy(showing = None, text = newText)
      end if
    end changeText

    override protected def changeSelection(move: Int) =
      showing match
        case None => this // do nothing, no alternatives are showing
        case a @ Some((filtered, showing)) =>
          val position = filtered.indexOf(showing)

          val newSelected =
            (position + move).max(0).min(filtered.length - 1)

          copy(showing = a.map(_ => (filtered, filtered(newSelected))))

  end State
end InteractiveMultipleChoice
