package com.indoorvivants.proompts

import ANSI.*

class InteractiveAlternatives(
    prompt: Prompt.Alternatives,
    writer: String => Unit
):
  val terminal = Terminal.ansi(writer)
  val lab      = prompt.promptLabel
  var state    = prompt.state

  def printPrompt() =
    val lines = 0

    import terminal.*

    moveHorizontalTo(0)
    eraseToEndOfLine()

    errln(prompt)

    writer(s"${fansi.Color.Cyan(lab)}${state.text}")
    withRestore:
      writer("\n")

      val filteredAlts =
        prompt.alts.filter(
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
        moveHorizontalTo(0)
        eraseToEndOfLine()
        writer(fansi.Underlined.On("no matches").toString)
      else
        filteredAlts.zipWithIndex.foreach: (alt, idx) =>
          moveHorizontalTo(0)
          eraseToEndOfLine()
          val view =
            if idx == adjustedSelected then fansi.Color.Green("> " + alt)
            else fansi.Bold.On("· " + alt)
          writer(view.toString)
          if idx != filteredAlts.length - 1 then writer("\n")
      end if

      for _ <- 0 until state.showing - newState.showing do
        moveNextLine(1)
        moveHorizontalTo(0)
        eraseToEndOfLine()
      state = newState
  end printPrompt

  val handler = new Handler:
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

  def selectUp() = state = state.copy(selected = (state.selected - 1).max(0))

  def selectDown() = state =
    state.copy(selected = (state.selected + 1).min(1000))

  def appendText(t: Char) =
    state = state.copy(text = state.text + t)

  def trimText() =
    state = state.copy(text = state.text.take(state.text.length - 1))

end InteractiveAlternatives

