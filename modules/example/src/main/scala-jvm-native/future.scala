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

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import cue4s.*

import concurrent.ExecutionContext.Implicits.global

@main def future =
  case class Info(
      day: Option[String] = None,
      work: Option[String] = None,
      letters: Set[String] = Set.empty
  )

  val prompts = Prompts()

  val fut = for
    day <- prompts
      .future(
        Prompt.SingleChoice("How was your day?", List("great", "okay"))
      )
      .map(_.toOption)

    work <- prompts.future(Prompt.Input("Where do you work?")).map(_.toOption)

    letters <- prompts
      .future(
        Prompt.MultipleChoice.withAllSelected(
          "What are your favourite letters?",
          ('A' to 'F').map(_.toString).toList
        )
      )
      .map(_.toOption)

    info = Info(day, work, letters.fold(Set.empty)(_.toSet))
  yield println(info)

  Await.result(fut, Duration.Inf)
end future
