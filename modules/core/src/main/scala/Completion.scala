package com.indoorvivants.proompts

enum Completion:
  case Finished
  case Interrupted
  case Error(msg: String)
