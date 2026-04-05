package cue4s

import cue4s.CharCollector.*

class CharCollectorTests extends munit.FunSuite:

  test("CSI Z (ESC[Z) decodes to SHIFT_TAB") {
    val (state1, _)       = decode(State.Init, 27) // ESC
    val (state2, _)       = decode(state1, '[')    // [
    val (state3, result3) = decode(state2, 'Z')    // Z
    assertEquals(state3, State.Init)
    assertEquals(result3, TerminalEvent.Key(KeyEvent.SHIFT_TAB))
  }

  test("ScanCode_Started with 15 decodes to SHIFT_TAB") {
    val (state, result) = decode(State.ScanCode_Started, 15)
    assertEquals(state, State.Init)
    assertEquals(result, TerminalEvent.Key(KeyEvent.SHIFT_TAB))
  }
end CharCollectorTests
