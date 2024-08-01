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

package example.sync

import cue4s.*

case class Info(
    day: Option[String] = None,
    work: Option[String] = None,
    letters: Set[String] = Set.empty
)

@main def sync =
  var info = Info()

  val prompts = Prompts()

  val day = prompts
    .sync(
      Prompt.SingleChoice("How was your day?", List("great", "okay"))
    )
    .toResult
  info = info.copy(day = day)

  val work = prompts.sync(Prompt.Input("Where do you work?")).toResult
  info = info.copy(work = work)

  val letters = prompts
    .sync(
      Prompt.MultipleChoice(
        "What are your favourite letters?",
        ('A' to 'F').map(_.toString).toList
      )
    )
    .toResult
  info = info.copy(letters = letters.fold(Set.empty)(_.toSet))

  println(info)
end sync
