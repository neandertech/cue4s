package cue4s

import com.indoorvivants.snapshots.munit_integration.MunitSnapshotsIntegration

class TextWrapTests extends munit.FunSuite, MunitSnapshotsIntegration:

  import TextWrap.greedy as wrap

  test("wrap text") {
    val text =
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit"

    val results =
      for
        width <- List(5, 10, 30, 100, 500)
        wrapped = wrap(text, width).mkString("\n")
      yield s"WIDTH=$width\n\n$wrapped"

    assertSnapshot(
      "wrap text",
      (s"ORIGINAL\n\n$text" :: results).mkString("\n\n"),
    )
  }
end TextWrapTests
