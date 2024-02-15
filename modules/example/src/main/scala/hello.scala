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

package com.indoorvivants.proompts
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@main def hello =
  val out      = Output.Std
  val terminal = Terminal.ansi(out)
  val colors   = true

  PromptChain
    .future(
      Prompt.Alternatives(
        "How is your day?",
        List("great", "okay", "shite")
      ),
      s => Future.successful(List(s)),
      terminal,
      out,
      colors
    )
    .andThen(
      day =>
        Future.successful(
          Prompt.Alternatives(
            s"So your day has been ${day}. And how was your poop",
            List("Strong", "Smelly")
          )
        ),
      (cur, poop) => Future.successful(poop :: cur)
    )
    .andThen(
      poop =>
        Future.successful(
          Prompt.Alternatives(
            s"I see... whatcha wanna do",
            List("Partay", "sleep")
          )
        ),
      (cur, doing) => Future.successful(doing :: cur)
    )
    .evaluateFuture.foreach: results =>
      println(results)

  // def nextPrompt(day: String) = Prompt.Alternatives(
  //   s"So your day has been ${day}. And how was your poop",
  //   List("Strong", "Smelly")
  // )

  // def interactive(prompt: Prompt) =
  //   Interactive(terminal, prompt, Output.Std, true)

  // val inputProvider = InputProvider(Output.Std)

  // inputProvider
  //   .evaluateFuture(interactive(prompt))
  //   .collect:
  //     case Completion.Finished(v) => v
  //   .flatMap: v =>
  //     inputProvider.evaluateFuture(interactive(nextPrompt(v)))

end hello
