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

import proompts.*, catseffect.*
import cats.effect.*

case class Info(
    day: Option[String] = None,
    work: Option[String] = None,
    weather: Option[String] = None
)
object ioExample extends IOApp.Simple:
  def run: IO[Unit] =
    PromptChain
      .io(Info())
      .prompt(
        _ =>
          AlternativesPrompt(
            "How is your day?",
            List("great", "okay", "shite")
          ),
        (info, day) => info.copy(day = Some(day))
      )
      .prompt(
        info =>
          AlternativesPrompt(
            s"So your day has been ${info.day.get}. How are things at work?",
            List("please go away", "I don't want to talk about it")
          ),
        (info, work) => info.copy(work = Some(work))
      )
      .prompt(
        _ =>
          AlternativesPrompt(
            s"Great! What fantastic weather we're having, right?",
            List("please leave me alone", "don't you have actual friends?")
          ),
        (cur, weather) => cur.copy(weather = Some(weather))
      )
      .evaluateIO
      .flatMap: results =>
        IO.println(results)
end ioExample
