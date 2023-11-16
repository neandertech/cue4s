package com.indoorvivants.proompts

enum Event:
  case Key(which: KeyEvent)
  case Char(which: Int)

enum KeyEvent:
  case UP, DOWN, LEFT, RIGHT
