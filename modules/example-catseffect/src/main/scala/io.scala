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
import cue4s.*

import cats.syntax.all.*

import catseffect.*

case class Info(
    day: String,
    work: String,
    letters: List[String],
)

object ioExample extends IOApp.Simple:
  def run: IO[Unit] =
    PromptsIO.make.use: prompts =>
      for
        _ <- IO.println("let's go")

        day = prompts
          .singleChoice("How was your day?", List("great", "okay"))
          .map(_.toEither)
          .flatMap(IO.fromEither)

        work = prompts
          .text("Where do you work?")
          .map(_.toEither)
          .flatMap(IO.fromEither)

        letter = prompts
          .multiChoiceAllSelected(
            "What are your favourite letters?",
            ('A' to 'F').map(_.toString).toList,
          )
          .map(_.toEither)
          .flatMap(IO.fromEither)

        info <- (day, work, letter).mapN(Info.apply)

        _ <- IO.println(info)
      yield ()

end ioExample
