package proompts

class ExampleTests extends munit.FunSuite:
  test("test1") {
    assertSnapshot("my.snapshot.things", "is this what you want?")
  }

end ExampleTests
