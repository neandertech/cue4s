package com.indoorvivants.proompts

enum Event:
  case Init
  case Key(which: KeyEvent)
  case Char(which: Int)
  case CSICode(bytes: List[Byte])

enum KeyEvent:
  case UP, DOWN, LEFT, RIGHT, ENTER, DELETE
