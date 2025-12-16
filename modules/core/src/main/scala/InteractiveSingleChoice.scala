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
    lab: String,
    alts: List[String],
    terminal: Terminal,
    out: Output,
    theme: Theme,
    windowSize: Int,
    symbols: Symbols,
) extends PromptFramework[String](terminal, out):
  import InteractiveSingleChoice.*

  override type PromptState = State
  override type Event       = TerminalEvent

  private lazy val altsWithIndex = alts.zipWithIndex
  private lazy val altMapping    = altsWithIndex.map(_.swap).toMap

  override def initialState = State(
    text = "",
    all = altsWithIndex,
    display = InfiniscrollableState(
      showing = Some(altsWithIndex.map(_._2) -> 0),
      windowStart = 0,
      windowSize = windowSize,
    ),
  )

  override def handleEvent(event: TerminalEvent) =
    event match
      case TerminalEvent.Key(KeyEvent.UP) =>
        PromptAction.updateState(_.updateDisplay(_.up))

      case TerminalEvent.Key(KeyEvent.DOWN) =>
        PromptAction.updateState(_.updateDisplay(_.down))

      case TerminalEvent.Key(KeyEvent.ENTER) => // enter
        currentState().display.showing match
          case None =>
            PromptAction.setStatus(
              Status.Running(Left(PromptError("nothing selected"))),
            )
          case Some((_, idx)) =>
            PromptAction.setStatus(Status.Finished(altMapping(idx)))

      case TerminalEvent.Key(KeyEvent.DELETE) => // enter
        PromptAction.updateState(_.trimText)

      case TerminalEvent.Char(which) =>
        PromptAction.updateState(_.addText(which.toChar))

      case TerminalEvent.Interrupt =>
        PromptAction.setStatus(Status.Canceled)

      case _ =>
        PromptAction.Continue
    end match
  end handleEvent

  import theme.*

  override def renderState(
      st: State,
      status: Status,
  ): List[String] =
    val lines = List.newBuilder[String]

    import symbols.*

    status match
      case Status.Running(_) | Status.Init =>
        // prompt question
        lines += "? ".focused + (lab + s" $promptCue ").prompt + st.text.input

        status match
          case Status.Running(err) =>
            err.foreach: err =>
              lines += err.error
          case _ =>

        st.display.showing match
          case None =>
            lines += "no matches...".noMatches
          case Some((filtered, selected)) =>
            // Render only the visible window
            st.display
              .visibleEntries(filtered)
              .zipWithIndex
              .foreach:
                case (id, idx) =>
                  val alt = altMapping(id)
                  lines.addOne(
                    if id == selected then s"  $altCursor $alt".focused
                    else if st.display.windowStart > 0 && idx == 0 then
                      s"  $pageUpArrow $alt".option
                    else if filtered.size > st.display.windowSize &&
                      idx == st.display.windowSize - 1 &&
                      filtered.indexOf(id) != filtered.size - 1
                    then s"  $pageDownArrow $alt".option
                    else s"    $alt".option,
                  )
        end match
      case Status.Finished(value) =>
        lines += s"$promptDone ".focused +
          (lab + " ").prompt +
          s" $ellipsis ".hint +
          value.emphasis
      case Status.Canceled =>
        lines += s"$promptCancelled ".canceled +
          (lab + " ").prompt
    end match

    lines.result()
  end renderState
end InteractiveSingleChoice

private[cue4s] object InteractiveSingleChoice:
  case class State(
      text: String,
      all: List[(String, Int)],
      display: InfiniscrollableState,
  ):
    def updateDisplay(f: InfiniscrollableState => InfiniscrollableState) =
      copy(display = f(display))

    def addText(t: Char) =
      val newText = text + t
      changeText(newText).updateDisplay(_.resetWindow())

    def trimText =
      changeText(text.dropRight(1)).updateDisplay(_.resetWindow())

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
end InteractiveSingleChoice
