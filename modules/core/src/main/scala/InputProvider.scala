package com.indoorvivants.proompts

enum Completion:
  case Finished
  case Interrupted
  case Error(msg: String)

enum Next:
  case Stop, Continue 
  case Error(msg: String)

trait InputProvider extends AutoCloseable:
  def attach(handler: Event => Next): Completion

object InputProvider extends InputProviderPlatform
