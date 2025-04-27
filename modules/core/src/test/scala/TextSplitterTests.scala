package cue4s

class TextSplitterTests extends munit.FunSuite:
  test("splitting") {
    val splitter = TextSplitter
    val text     = "hello world, I say"
    val result   = splitter.split(text, 5)
    assertEquals(result, List("hello", "world,", "I say"))

    assertEquals(splitter.split(text, text.length), List(text))

    assertEquals(splitter.split("", 0), List(""))
    assertEquals(splitter.split("hello", 5), List("hello"))
    assertEquals(splitter.split("hello", 3), List("hello"))
  }
end TextSplitterTests
