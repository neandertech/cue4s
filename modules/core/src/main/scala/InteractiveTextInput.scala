package com.indoorvivants.proompts

import ANSI.*

class InteractiveTextInput(
    prompt: Prompt.Input,
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
  end printPrompt

  val handler = new Handler:
    def apply(event: Event): Next =
      errln(event)
      event match
        case Event.Init =>
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

  def appendText(t: Char) =
    state = state.copy(text = state.text + t)

  def trimText() =
    state = state.copy(text = state.text.take(state.text.length - 1))
end InteractiveTextInput
