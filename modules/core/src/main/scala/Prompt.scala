package com.indoorvivants.proompts

import ANSI.*

enum Prompt(label: String):
  case Input(label: String, state: TextInputState) extends Prompt(label)
  case Alternatives(label: String, alts: List[String], state: AlternativesState)
      extends Prompt(label)

  def promptLabel =
    label + " > "

object Prompt:
  object Alternatives:
    def apply(label: String, alts: List[String]): Prompt =
      Prompt.Alternatives(label, alts, AlternativesState("", 0, alts.length))
