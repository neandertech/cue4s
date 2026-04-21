/*
 * Copyright 2023 Neandertech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cue4s

import cue4s.Transition.Changed

private[cue4s] trait Transition[R]:
  def current: R
  def last: Option[R]
  def next(r: R): Changed
  def nextFn(r: R => R): Changed

  def changed: Boolean =
    !last.contains(current)

private[cue4s] object Transition:
  enum Changed:
    case Yes, No

  class RefTransition[R](init: R) extends Transition[R]:
    private var _current: R      = init
    private var _last: Option[R] = None

    override def current: R =
      if _current == null then
        _current = init
        _current.nn
      else _current.nn

    override def last: Option[R] = _last

    override def next(r: R): Changed =
      if r == current then Changed.No
      else
        this.synchronized:
          _last = Some(current)
          _current = r

        Changed.Yes

    override def nextFn(r: R => R): Changed =
      val newRes = r(current)
      if newRes == current then Changed.No
      else
        this.synchronized:
          _last = Some(current)
          _current = newRes
        Changed.Yes

  end RefTransition

  class FollowTransition[R](t: Transition[R]) extends Transition[R]:
    override def current: R                 = t.current
    override def last: Option[R]            = t.last
    override def next(r: R): Changed        = t.next(r)
    override def nextFn(r: R => R): Changed = t.nextFn(r)

  def base[R](current: R): Transition[R] =
    new RefTransition(current)

  def follow[R](t: Transition[R]): Transition[R] =
    new FollowTransition(t)

  class ReadOnlyTransition[R](t: Transition[R]) extends Transition[R]:
    override def current: R                 = t.current
    override def last: Option[R]            = t.last
    override def next(r: R): Changed        = Changed.No
    override def nextFn(r: R => R): Changed = Changed.Yes
    override def changed: Boolean           = t.changed

  def readOnly[R](t: Transition[R]): Transition[R] =
    new ReadOnlyTransition(t)

end Transition

//   class BaseTransition[R](private var _current: => R, val last: Option[R] = None)
//       extends Transition[R]:
//     override lazy val current: R = _current
//     override def next(r: R): Transition[R] =
//       new BaseTransition(_current = r, last = Some(current))
//     override def nextFn(r: R => R): Transition[R] =
//       new BaseTransition(_current = r(current), last = Some(current))

// end Transition

private[cue4s] class LoggedTransition[R] private (
    label: String,
    t: Transition[R],
    out: Output,
) extends Transition[R]:
  export t.{current, last}
  override def next(r: R): Changed =
    // out.logLn(s"Transition [$label] (set): $current changed to $r")
    out.logLn(s"Transition [$label] (set):")
    out.logLn(s"  current: $current")
    out.logLn(s"  new: $r")
    t.next(r)

  override def nextFn(r: R => R): Changed =
    val cur        = current
    val transition = t.nextFn(r)
    if cur != t.current then
      out.logLn(s"Transition [$label] (mapping):")
      out.logLn(s"  current: $cur")
      out.logLn(s"  new: $current")

    transition
  end nextFn

end LoggedTransition

private[cue4s] object LoggedTransition:
  def apply[R](label: String, t: Transition[R], out: Output): Transition[R] =
    new LoggedTransition(label, t, out)
