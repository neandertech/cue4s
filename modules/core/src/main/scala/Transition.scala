package cue4s

private[cue4s] case class Transition[R](current: R, last: Option[R] = None):
  def next(r: R)        = copy(current = r, last = Some(current))
  def nextFn(r: R => R) = copy(current = r(current), last = Some(current))
