package com.indoorvivants.proompts

enum Next:
  case Stop, Continue
  case Error(msg: String)
