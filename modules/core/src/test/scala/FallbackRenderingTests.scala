package cue4s

import cue4s.*
import cue4s.Prompt.PasswordInput.Password

import KeyEvent.*

class FallbackRenderingTests extends munit.FunSuite, TerminalTests:

  terminalTestComplete("fallback.render.alternatives")(
    Prompt.SingleChoice(
      "How was your day?",
      List(
        "amazing",
        "productive",
        "relaxing",
        "stressful",
        "exhausting",
      ),
      windowSize = 3,
    ),
    list(
      Event.Init,
      DOWN,
      DOWN,
      DOWN,
      DOWN,
      UP,
      ENTER,
    ),
    "stressful",
    symbols = Symbols.ASCIISymbols,
  )

  terminalTestComplete("fallback.alternatives.multiple")(
    Prompt.MultipleChoice.withNoneSelected(
      "What would you like for lunch",
      List("pizza", "steak", "sweet potato", "fried chicken", "sushi"),
      windowSize = 3,
    ),
    list(
      Event.Init,
      TAB,
      DOWN,
      TAB,
      DOWN,
      DOWN,
      TAB,
      DOWN,
      TAB,
      ENTER,
    ),
    List("pizza", "steak", "fried chicken", "sushi"),
    symbols = Symbols.ASCIISymbols,
  )

  terminalTestComplete("fallback.confirm")(
    Prompt.Confirmation("Are you sure?", default = true),
    list(
      Event.Init,
      Event.Char('n'),
    ),
    false,
    symbols = Symbols.ASCIISymbols,
  )

  terminalTestComplete("fallback.input")(
    Prompt
      .Input("how do you do fellow kids?")
      .validate(value =>
        if value.length < 4 then Some(PromptError("too short!"))
        else None,
      ),
    list(
      Event.Init,
      chars("go"),
      ENTER, // prevents submission
      chars("od"),
      ENTER,
    ),
    "good",
    symbols = Symbols.ASCIISymbols,
  )
end FallbackRenderingTests
