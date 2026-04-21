package cue4s

import com.indoorvivants.snapshots.munit_integration.MunitSnapshotsIntegration

class TextWrapTests extends munit.FunSuite, MunitSnapshotsIntegration:

  import TextWrap.greedy as wrap

  extension (lines: List[String])
    def withColumn(maxLength: Int): String =
      lines
        .map: l =>
          val (head, tail) = l.splitAt(maxLength)
          head.padTo(maxLength, ' ') + "|" + tail
        .mkString("\n")

  test("split") {
    assertEquals(
      TextWrap
        .splitAtWhitespace(
          fansi.Str("Lorem       ipsum   dolor sit    amet bla    "),
        )
        .map(_.render),
      List("Lorem", "ipsum", "dolor", "sit", "amet", "bla"),
    )
  }
  test("wrap text") {
    val text =
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit.asdasdasdHello/      "

    val results =
      for
        width <- List(5, 10, 30, 100, 500)
        wrapped = wrap(text, width).map(_.render).withColumn(width)
      yield s"WIDTH=$width\n\n$wrapped"

    assertSnapshot(
      "wrap text",
      (s"ORIGINAL\n\n$text" :: results).mkString("\n\n"),
    )
  }
end TextWrapTests
