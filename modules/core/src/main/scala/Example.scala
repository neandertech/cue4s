package com.indoorvivants.proompts

import ANSI.*

enum Prompt(label: String):
  case Input(label: String, state: InputState) extends Prompt(label)
  case Alternatives(label: String, alts: List[String], state: AlternativesState)
      extends Prompt(label)

  def promptLabel =
    label + " > "

object Prompt:
  object Alternatives:
    def apply(label: String, alts: List[String]): Prompt =
      Prompt.Alternatives(label, alts, AlternativesState("", 0, alts.length))

case class InputState(text: String)
case class AlternativesState(
    text: String,
    selected: Int,
    showing: Int
)

@main def hello =

  var prompt = Prompt.Alternatives(
    "How would you describe yourself?",
    List("Sexylicious", "Shmexy", "Pexying")
  )

  def printPrompt() =
    val lab   = prompt.promptLabel
    val lines = 0
    print(move.horizontalTo(0))
    print(erase.line.toEndOfLine())
    prompt match
      case Prompt.Input(label, state) =>
        print(Console.CYAN + lab + Console.RESET + state.text)
      case Prompt.Alternatives(label, alts, state) =>
        print(Console.CYAN + lab + Console.RESET + state.text)
        withRestore:
          print("\n")

          val filteredAlts =
            alts.filter(
              state.text.isEmpty() || _.toLowerCase().contains(state.text.toLowerCase())
            )

          val adjustedSelected =
            state.selected.min(filteredAlts.length - 1).max(0)

          val newState =
            AlternativesState(
              state.text,
              selected = adjustedSelected,
              showing = filteredAlts.length.min(1)
            )

          if filteredAlts.isEmpty then
            print(move.horizontalTo(0))
            print(erase.line.toEndOfLine())
            print(fansi.Underlined.On("no matches"))
          else
            filteredAlts.zipWithIndex.foreach: (alt, idx) =>
              print(move.horizontalTo(0))
              print(erase.line.toEndOfLine())

              val view =
                if idx == adjustedSelected then fansi.Color.Green("> " + alt)
                else fansi.Bold.On("· " + alt)
              print(view)
              if idx != filteredAlts.length - 1 then print("\n")
          end if

          for _ <- 0 until state.showing - newState.showing do
            print(move.nextLine(1))
            print(move.horizontalTo(0))
            print(erase.line.toEndOfLine())
    end match
  end printPrompt

  printPrompt()

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

  InputProvider().attach:
    case Event.Key(KeyEvent.UP) =>
      selectUp()
      printPrompt()
      Next.Continue
    case Event.Key(KeyEvent.DOWN) =>
      selectDown()
      printPrompt()
      Next.Continue

    case Event.Char(10) => // enter
      println("booyah!")
      Next.Stop

    case Event.Char(127) => // enter
      trimText()
      printPrompt()
      Next.Continue

    case Event.Char(which) =>
      appendText(which.toChar)
      printPrompt()
      Next.Continue
    case _ => Next.Continue
end hello
