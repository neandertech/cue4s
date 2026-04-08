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

package cue4s_example

import cue4s.*
import cue4s.Prompt.PasswordInput.Password

@main def sync(which: String*) =
  Prompts.sync.use: prompts =>
    val examples = Map[String, (String, SyncPrompts => Unit)](
      "confirm"       -> ("Yes/No confirmation", likeCats),
      "single-choice" -> ("Single choice", day),
      "multi-choice"  -> ("Multiple choice", letters),
      "validated-int" -> ("Validated integer", seasons),
      "password"      -> ("Password", password),
    )

    if which.isEmpty then
      val maxLength = examples.keys.map(_.length).max

      val selected = prompts
        .multiChoiceNoneSelected(
          "Which examples would you like to run?",
          examples
            .map { case (k, (desc, _)) =>
              s"${k.padTo(maxLength, ' ')}: $desc"
            }
            .toList
            .sorted,
        )
        .getOrThrow

      val toShow = selected.map(_.split(": ").head.trim)
      toShow.foreach { k => examples(k)._2(prompts) }
    else
      val allowed = examples.keySet
      val missing = which.toSet -- allowed

      if missing.nonEmpty then
        sys.error(s"Unknown example names: ${missing
            .mkString(", ")}. Available examples: ${allowed.mkString(", ")}")
      else which.foreach { k => examples(k)._2(prompts) }
    end if

end sync

def likeCats(prompts: SyncPrompts) = prompts
  .confirm("Do you like cats?")
  .toOption

def day(prompts: SyncPrompts) = prompts
  .singleChoice(
    "How was your day?",
    List(
      "amazing",
      "productive",
      "relaxing",
      "stressful",
      "exhausting",
      "challenging",
      "wonderful",
      "uneventful",
      "interesting",
      "exciting",
      "boring",
      "demanding",
      "satisfying",
      "frustrating",
      "peaceful",
      "overwhelming",
      "busy",
      "calm",
      "enjoyable",
      "memorable",
      "ordinary",
      "fantastic",
      "rewarding",
      "chaotic",
    ),
    _.withWindowSize(7),
  )
  .toOption

def skyColor(prompts: SyncPrompts): Completion[Boolean] = prompts.run(
  Prompt
    .Input("What color is the sky?")
    .mapValidated: s =>
      Either.cond(s == "blue", true, PromptError("hint: it's blue")),
)

def letters(prompts: SyncPrompts): Completion[List[String]] =
  prompts.multiChoiceNoneSelected(
    "What are your favourite letters?",
    ('A' to 'Z').map(_.toString).toList,
    _.withWindowSize(7),
  )

def seasons(prompts: SyncPrompts): Completion[Int] =
  prompts.int(
    "How many seasons of Stargate SG-1 are there",
    _.validate:
      case x if x < 10 => Option(PromptError("More!"))
      case x if x > 10 => Option(PromptError("Fewer!"))
      case _           => None,
  )

def password(prompts: SyncPrompts): Completion[Password] =
  prompts.password("Choose a new password")
