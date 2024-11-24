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
  import InteractiveSingleChoice.*

  val handler = new Handler[String]:
    def apply(event: Event): Next[String] =
      event match
        case Event.Init =>
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

        case Event.Key(KeyEvent.ENTER) => // enter
          stateTransition(_.finish)
          state.current.status match
            case Status.Running => Next.Continue
            case Status.Finished(idx) =>
              val stringValue = altMapping(idx)
              printPrompt()
              Next.Done(stringValue)
            case Status.Canceled => Next.Stop

        case Event.Key(KeyEvent.DELETE) => // enter
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
  end handler

  private val altsWithIndex = prompt.alts.zipWithIndex
  private var state = Transition(
    State("", Some(altsWithIndex.map(_._2) -> 0), altsWithIndex, Status.Running)
  )
  private var rendering       = Transition(renderState(state.current))
  private lazy val altMapping = altsWithIndex.map(_.swap).toMap

  private def colored(msg: String)(f: String => fansi.Str) =
    if colors then f(msg).toString else msg

  private def renderState(st: State): List[String] =
    val lines = List.newBuilder[String]

    st.status match
      case Status.Running =>
        // prompt question
        lines += "· " + colored(prompt.lab + " > ")(
          fansi.Color.Cyan(_)
        ) + state.current.text

        st.showing match
          case None =>
            lines += colored("no matches...")(fansi.Bold.On(_))
          case Some((filtered, selected)) =>
            filtered.foreach: id =>
              val alt = altMapping(id)
              lines.addOne(
                if id == selected then
                  colored(s"  ‣ $alt")(fansi.Color.Green(_))
                else colored(s"    $alt")(fansi.Bold.On(_))
              )
        end match
      case Status.Finished(idx) =>
        val value = altMapping(idx)
        lines += colored("✔ ")(fansi.Color.Green(_)) +
          colored(prompt.lab + " ")(fansi.Color.Cyan(_)) +
          colored(value)(fansi.Bold.On(_))
        lines += ""
      case Status.Canceled =>
        lines += colored("× ")(fansi.Color.Red(_)) +
          colored(prompt.lab + " ")(fansi.Color.Cyan(_))
        lines += ""

    end match

    lines.result()
  end renderState

  private def printPrompt() =
    import terminal.*
    cursorHide()
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

end InteractiveSingleChoice

object InteractiveSingleChoice:
  enum Status:
    case Running
    case Finished(idx: Int)
    case Canceled

  case class State(
      text: String,
      showing: Option[(List[Int], Int)],
      all: List[(String, Int)],
      status: Status
  ):
    def finish =
      showing match
        case None => this
        case Some((_, selected)) =>
          copy(status = Status.Finished(selected))
    def cancel = copy(status = Status.Canceled)

    def up   = changeSelection(-1)
    def down = changeSelection(+1)

    def addText(t: Char) =
      changeText(text + t)
    end addText

    def trimText =
      changeText(text.dropRight(1))

    private def changeSelection(move: Int) =
      showing match
        case None => this // do nothing, no alternatives are showing
        case a @ Some((filtered, showing)) =>
          val position = filtered.indexOf(showing)

          val newSelected =
            (position + move).max(0).min(filtered.length - 1)

          copy(showing = a.map(_ => (filtered, filtered(newSelected))))

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

  end State
end InteractiveSingleChoice
