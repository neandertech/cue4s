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
    // prompt: Prompt.MultipleChoice,
    lab: String,
    alts: List[(String, Boolean)],
    terminal: Terminal,
    out: Output,
    theme: Theme,
    windowSize: Int,
    symbols: Symbols,
) extends PromptFramework[List[String]](terminal, out):
  import InteractiveMultipleChoice.*

  override type PromptState = State

  private lazy val preSelected =
    alts.zipWithIndex.filter(_._1._2).map(_._2)
  private lazy val altsWithIndex = alts.map(_._1).zipWithIndex

  override def initialState = State(
    text = "",
    selected = preSelected.toSet,
    all = altsWithIndex,
    display = InfiniscrollableState(
      showing = Some(altsWithIndex.map(_._2) -> 0),
      windowStart = 0,
      windowSize = windowSize,
    ),
  )

  private lazy val altMapping = altsWithIndex.map(_.swap).toMap

  import theme.*

  override def renderState(
      st: State,
      status: Status,
  ): List[String] =
    val lines = List.newBuilder[String]

    import symbols.*

    status match
      case Status.Running(_) | Status.Init =>
        lines += "? ".focused + lab.prompt + s" $promptCue ".prompt + st.text.input
        lines += "Tab".emphasis + " to toggle, " + "Enter".emphasis + " to submit."

        status match
          case Status.Running(Left(err)) =>
            lines += err.error
          case _ =>

        st.display.showing match
          case None =>
            lines += "no matches...".noMatches
          case Some((filtered, selected)) =>
            st.display
              .visibleEntries(filtered)
              .zipWithIndex
              .foreach:
                case (id, idx) =>
                  val alt = altMapping(id)
                  if st.selected(id) then
                    if id == selected then
                      lines += s" $altSelected " + alt.selectedMany
                    else lines += s" $altSelected " + alt.selectedManyInactive
                  else
                    lines.addOne(
                      if id == selected then s" $altNotSelected $alt".focused
                      else if st.display.windowStart > 0 && idx == 0 then
                        s" $pageUpArrow $alt".optionMany
                      else if filtered.size > st.display.windowSize && idx == st.display.windowSize - 1 &&
                        filtered.indexOf(id) != filtered.size - 1
                      then s" $pageDownArrow $alt".optionMany
                      else s" $altNotSelected $alt".optionMany,
                    )
                  end if
        end match

      case Status.Finished(ids) =>
        lines += s"$promptDone ".focused + lab.prompt

        if ids.isEmpty then lines += "nothing selected".nothingSelected
        else
          ids
            .foreach: value =>
              lines += s" $altSelected " + value.emphasis

      case Status.Canceled =>
        lines += s"$promptCancelled ".canceled +
          (lab + " ").prompt
        lines += ""
    end match

    lines.result()
  end renderState

  override def handleEvent(event: Event) =
    event match
      case Event.Key(KeyEvent.UP) =>
        PromptAction.updateState(_.updateDisplay(_.up))

      case Event.Key(KeyEvent.DOWN) =>
        PromptAction.updateState(_.updateDisplay(_.down))

      case Event.Key(KeyEvent.ENTER) =>
        PromptAction.setStatus(
          Status.Finished(
            currentState().selected.toList.sorted.map(altMapping).toList,
          ),
        )

      case Event.Key(KeyEvent.TAB) =>
        PromptAction.updateState(_.toggle)

      case Event.Key(KeyEvent.DELETE) =>
        PromptAction.updateState(_.trimText)

      case Event.Char(which) =>
        PromptAction.updateState(_.addText(which.toChar))

      case Event.Interrupt =>
        PromptAction.setStatus(Status.Canceled)

      case _ =>
        PromptAction.Continue
    end match
  end handleEvent

end InteractiveMultipleChoice

private[cue4s] object InteractiveMultipleChoice:
  case class State(
      text: String,
      selected: Set[Int],
      all: List[(String, Int)],
      display: InfiniscrollableState,
  ):

    def updateDisplay(f: InfiniscrollableState => InfiniscrollableState) =
      copy(display = f(display))

    def addText(t: Char) =
      changeText(text + t).updateDisplay(_.resetWindow())

    def trimText =
      changeText(text.dropRight(1)).updateDisplay(_.resetWindow())

    def toggle =
      display.showing match
        case None => this
        case Some((_, cursor)) =>
          if selected(cursor) then copy(selected = selected - cursor)
          else copy(selected = selected + cursor)

    private def changeText(newText: String) =
      val newFiltered = all.filter((alt, _) =>
        newText.trim.isEmpty || alt
          .toLowerCase()
          .contains(newText.toLowerCase().trim()),
      )
      if newFiltered.nonEmpty then
        display.showing match
          case None =>
            val newShowing = newFiltered.headOption.map: (_, id) =>
              newFiltered.map(_._2) -> id

            updateDisplay(_.copy(showing = newShowing)).copy(text = newText)

          case Some((_, selected)) =>
            val newShowing = newFiltered.headOption.map: (_, id) =>
              val newSelected =
                newFiltered.find(_._2 == selected).map(_._2).getOrElse(id)
              newFiltered.map(_._2) -> newSelected

            updateDisplay(_.copy(showing = newShowing)).copy(text = newText)
      else updateDisplay(_.copy(showing = None)).copy(text = newText)
      end if
    end changeText

  end State
end InteractiveMultipleChoice
