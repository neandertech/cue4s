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

package example.catseffect

import cats.effect.*
import proompts.*

import catseffect.*

case class Info(
    day: Option[String] = None,
    work: Option[String] = None,
    letters: Set[String] = Set.empty
)

object ioExample extends IOApp.Simple:
  def run: IO[Unit] =
    for
      ref <- IO.ref(Info())

      day <- RunPrompt
        .io(
          Prompt.SingleChoice("How was your day?", List("great", "okay"))
        )
        .map(_.toResult)
        .flatTap(day => ref.update(_.copy(day = day)))

      work <- RunPrompt
        .io(
          Prompt.Input("Where do you work?")
        )
        .map(_.toResult)
        .flatTap(work => ref.update(_.copy(work = work)))

      letter <- RunPrompt
        .io(
          Prompt.MultipleChoice(
            "What are your favourite letters?",
            ('A' to 'F').map(_.toString).toList
          )
        )
        .map(_.toResult)
        .flatTap(letter =>
          ref.update(_.copy(letters = letter.fold(Set.empty)(_.toSet)))
        )

      _ <- ref.get.flatMap(IO.println)
    yield ()

end ioExample
