package com.indoorvivants.proompts

import ANSI.*
import com.indoorvivants.proompts.ANSI.move.horizontalTo

def errln(o: Any) = System.err.println(o)

class Interactive(var prompt: Prompt, writer: String => Unit):

  def printPrompt() =
    val lab   = prompt.promptLabel
    val lines = 0
    writer(move.horizontalTo(0))
    writer(erase.line.toEndOfLine())

    errln(prompt)
    errln(prompt.hashCode())
    prompt match
      case Prompt.Input(label, state) =>
        writer(s"${fansi.Color.Cyan(lab)}${state.text}")
      case p @ Prompt.Alternatives(label, alts, state) =>
        writer(s"${fansi.Color.Cyan(lab)}${state.text}")
        withRestore(writer):
          writer("\n")

          val filteredAlts =
            alts.filter(
              state.text.isEmpty() || _.toLowerCase().contains(
                state.text.toLowerCase()
              )
            )

          errln(filteredAlts)

          val adjustedSelected =
            state.selected.min(filteredAlts.length - 1).max(0)

          errln(adjustedSelected)

          val newState =
            AlternativesState(
              state.text,
              selected = adjustedSelected,
              showing = filteredAlts.length.min(1)
            )

          if filteredAlts.isEmpty then
            writer(move.horizontalTo(0))
            writer(erase.line.toEndOfLine())
            writer(fansi.Underlined.On("no matches").toString)
          else
            filteredAlts.zipWithIndex.foreach: (alt, idx) =>
              writer(move.horizontalTo(0))
              writer(erase.line.toEndOfLine())

              val view =
                if idx == adjustedSelected then fansi.Color.Green("> " + alt)
                else fansi.Bold.On("· " + alt)
              writer(view.toString)
              if idx != filteredAlts.length - 1 then writer("\n")
          end if

          for _ <- 0 until state.showing - newState.showing do
            writer(move.nextLine(1))
            writer(move.horizontalTo(0))
            writer(erase.line.toEndOfLine())
          prompt = p.copy(state = newState)
    end match
  end printPrompt

  def selectUp() = prompt match
    case Prompt.Input(_, _) =>
    case p @ Prompt.Alternatives(_, _, state) =>
      prompt =
        p.copy(state = state.copy(selected = (state.selected - 1).max(0)))

  def selectDown() = prompt match
    case Prompt.Input(_, _) =>
    case p @ Prompt.Alternatives(_, _, state) =>
      prompt =
        p.copy(state = state.copy(selected = (state.selected + 1).min(1000)))

  def appendText(t: Char) =
    prompt match
      case i @ Prompt.Input(label, state) =>
        prompt = i.copy(state = state.copy(text = state.text + t))
      case i @ Prompt.Alternatives(label, alts, state) =>
        prompt = i.copy(state = state.copy(text = state.text + t))

  def trimText() =
    prompt match
      case i @ Prompt.Input(label, state) =>
        prompt = i.copy(state =
          state.copy(text = state.text.take(state.text.length - 1))
        )
      case i @ Prompt.Alternatives(label, alts, state) =>
        prompt = i.copy(state =
          state.copy(text = state.text.take(state.text.length - 1))
        )

  def handler = new Handler:
    def apply(event: Event): Next =
      errln(event)
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
end Interactive

case class InputState(text: String)
case class AlternativesState(
    text: String,
    selected: Int,
    showing: Int
)

