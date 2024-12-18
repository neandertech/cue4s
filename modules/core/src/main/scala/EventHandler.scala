package cue4s

abstract class EventHandler[Result]:
  def apply(ev: Event): Next[Result]
