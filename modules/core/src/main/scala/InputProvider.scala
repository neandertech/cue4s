package com.indoorvivants.proompts

case class Environment(writer: String => Unit)

abstract class Handler:
  def apply(ev: Event): Next

trait InputProvider extends AutoCloseable:
  def attach(env: Environment => Handler): Completion

object InputProvider extends InputProviderPlatform
