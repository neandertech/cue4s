package com.indoorvivants.proompts

import scala.concurrent.Future

trait InputProviderPlatform:
  self: InputProvider =>

  def evaluate(f: Interactive): Completion
  def evaluateFuture(f: Interactive): Future[Completion]

trait InputProviderCompanionPlatform:
  def apply(o: Output): InputProvider = InputProviderImpl(o)

end InputProviderCompanionPlatform
