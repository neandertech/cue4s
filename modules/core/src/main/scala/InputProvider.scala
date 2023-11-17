package com.indoorvivants.proompts

enum Completion:
  case Finished
  case Interrupted
  case Error(msg: String)

enum Next:
  case Stop, Continue
  case Error(msg: String)

case class Environment(writer: String => Unit)

abstract class Handler:
  def apply(ev: Event): Next

trait InputProvider extends AutoCloseable:
  def attach(env: Environment => Handler): Completion

object InputProvider extends InputProviderPlatform
