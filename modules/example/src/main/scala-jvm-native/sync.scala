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

@main def sync =
  Prompts.use(): prompts =>
    val likeCats = prompts
      .sync(
        Prompt.Confirmation("Do you like cats?"),
      )
      .toOption

    val day = prompts
      .sync(
        Prompt.SingleChoice(
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
          windowSize = 7,
        ),
      )
      .toOption

    val skyColor: Completion[Boolean] = prompts.sync(
      Prompt
        .Input("What color is the sky?")
        .mapValidated: s =>
          Either.cond(s == "blue", true, PromptError("hint: it's blue")),
    )

    val letters: Completion[List[String]] = prompts
      .sync(
        Prompt.MultipleChoice.withNoneSelected(
          "What are your favourite letters?",
          ('A' to 'Z').map(_.toString).toList,
          windowSize = 7,
        ),
      )

    val seasons: Completion[Int] =
      prompts.sync(
        Prompt.NumberInput
          .int("How many seasons of Stargate SG-1 are there")
          .validate:
            case x if x < 10 => Option(PromptError("More!"))
            case x if x > 10 => Option(PromptError("Fewer!"))
            case _           => None,
      )

    val password: Completion[Password] =
      prompts.sync(
        Prompt.PasswordInput("Choose a new password"),
      )

    println(s"$likeCats, $skyColor, $letters, $day, $seasons, $password")

end sync
