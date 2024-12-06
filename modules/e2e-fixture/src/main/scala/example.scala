import cue4s.*

import scala.concurrent.ExecutionContext.Implicits.global

@main def hello =
  await:
    Prompts.async.noColors.use: prompts =>
      prompts.text("Hello?")
