/*
 * Copyright 2023 Anton Sviridov
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

package example.future

import cue4s.*

import concurrent.ExecutionContext.Implicits.global

case class Info(
    day: Option[String] = None,
    work: Option[String] = None,
    letters: Set[String] = Set.empty
)

@main def future =
  val prompts = Prompts()

  for
    day <- prompts
      .future(
        Prompt.SingleChoice("How was your day?", List("great", "okay"))
      )
      .map(_.toResult)

    work <- prompts.future(Prompt.Input("Where do you work?")).map(_.toResult)

    letters <- prompts
      .future(
        Prompt.MultipleChoice(
          "What are your favourite letters?",
          ('A' to 'F').map(_.toString).toList
        )
      )
      .map(_.toResult)

    info = Info(day, work, letters.fold(Set.empty)(_.toSet))
  yield println(info)
  end for
end future
