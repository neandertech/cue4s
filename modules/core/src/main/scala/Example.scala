package com.indoorvivants.proompts

import ANSI.*

@main def hello =
  var prompt = Prompt.Alternatives(
    "How would you describe yourself?",
    List("Sexylicious", "Shmexy", "Pexying")
  )

  val result =
    InputProvider().attach(env => Interactive(prompt, env.writer).handler)

  // Process.stdout.write("how do you do")
  // Process.stdout.write(move.back(5))
  // Process.stdout.write(erase.line.toEndOfLine())
end hello
