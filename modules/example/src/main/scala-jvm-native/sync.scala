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
  Prompts.sync.use: prompts =>
    val likeCats = prompts
      .confirm("Do you like cats?")
      .toOption

    val day = prompts
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

    val skyColor: Completion[Boolean] = prompts.run(
      Prompt
        .Input("What color is the sky?")
        .mapValidated: s =>
          Either.cond(s == "blue", true, PromptError("hint: it's blue")),
    )

    val letters: Completion[List[String]] =
      prompts.multiChoiceNoneSelected(
        "What are your favourite letters?",
        ('A' to 'Z').map(_.toString).toList,
        _.withWindowSize(7),
      )

    val seasons: Completion[Int] =
      prompts.int(
        "How many seasons of Stargate SG-1 are there",
        _.validate:
          case x if x < 10 => Option(PromptError("More!"))
          case x if x > 10 => Option(PromptError("Fewer!"))
          case _           => None,
      )

    val password: Completion[Password] =
      prompts.password("Choose a new password")

end sync
