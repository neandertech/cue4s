package com.indoorvivants.proompts

enum Completion:
  case Finished, Interrupted

enum Next:
  case Stop, Continue

trait InputProvider extends AutoCloseable:
  def attach(handler: Event => Next): Completion

object InputProvider extends InputProviderPlatform
