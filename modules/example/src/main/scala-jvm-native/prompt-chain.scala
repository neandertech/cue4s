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

enum Opts:
  case Slap, Trap, Clap

case class Test(
    @cue(_.text("How's your day?"))
    x: String,
    @cue(_.validate(Test.validateY).text("Give me U"))
    y: String,
    z: Option[String],
    @cue(_.options("yes", "no", "don't know"))
    test: String,
    @cue(_.options("get", "post", "patch"))
    hello: List[String],
    @cue(_.options(Opts.values.map(_.toString).toSeq*))
    hello2: List[String],
) derives PromptChain

object Test:
  def validateY(y: String) =
    if y.trim.isEmpty() then Some(PromptError("cannot be empty!"))
    else if y.trim == "pasta" then Some(PromptError("stop talking about food"))
    else None

@main def promptChain =
  val prompts = Prompts()
  val result  = prompts.sync(PromptChain[Test])

  println(result)
