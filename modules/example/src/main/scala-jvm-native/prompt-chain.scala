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
    hello2: List[String]
) derives PromptChain

object Test:
  def validateY(y: String) =
    if y.trim.isEmpty() then Some(PromptError("cannot be empty!"))
    else if y.trim == "pasta" then Some(PromptError("stop talking about food"))
    else None

@main def promptChain =
  val prompts = Prompts()
  val result = prompts.sync(PromptChain[Test])

  println(result)
