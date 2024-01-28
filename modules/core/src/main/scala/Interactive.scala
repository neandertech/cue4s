package com.indoorvivants.proompts

import ANSI.*

def errln(o: Any) = System.err.println(o)

class Interactive(var prompt: Prompt, writer: String => Unit):

  val handler =
    prompt match
      case p: Prompt.Input        => InteractiveTextInput(p, writer).handler
      case p: Prompt.Alternatives => InteractiveAlternatives(p, writer).handler

end Interactive

case class TextInputState(text: String)
case class AlternativesState(
    text: String,
    selected: Int,
    showing: Int
)
