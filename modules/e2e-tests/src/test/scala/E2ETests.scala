package cue4s

import os.ProcessInput

class E2ETests extends munit.FunSuite:
  sys.env
    .filter(_._1.startsWith("CUE4S_EXAMPLE"))
    .foreach: (env, value) =>
      val tag      = env.stripPrefix("CUE4S_EXAMPLE_").toLowerCase()
      val testName = "e2e " + tag

      test(testName):
        val cmd =
          if tag == "js" then Seq("node", value)
          else if tag == "jvm" then Seq("java", "-jar", value)
          else Seq(value)

        println("Running " + cmd.mkString("`", " ", "`"))
        // no assertions on content because all platforms behave differently :-/
        assert(os.proc(cmd).call(stdin = "this world\n").exitCode == 0)

end E2ETests
