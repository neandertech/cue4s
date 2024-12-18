package cue4s

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

private trait InputProviderCompanionPlatform:
  def apply(o: Terminal): InputProvider = InputProviderImpl(o)
end InputProviderCompanionPlatform
