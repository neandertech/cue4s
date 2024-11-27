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

private[cue4s] trait InfiniscrollableState[A <: InfiniscrollableState[A]]:
  self: A =>

  def showing: Option[(List[Int], Int)]
  def windowStart: Int
  def windowSize: Int
  protected def changeSelection(move: Int): A

  // implement as copy(windowStart = computeWindowStartAfterSearch)
  // use it after any filtering operation
  protected def resetWindow(): A

  protected def scrolledUpWindowStart: Int = (windowStart - 1).max(0)

  // implement as copy(windowStart = scrolledUpWindowStart)
  protected def scrollUp: A

  protected def scrolledDownWindowStart: Int = windowStart + 1

  // implement as copy(windowStart = scrolledDownWindowStart)
  protected def scrollDown: A

  def atTopScrollingPoint(position: Int) =
    position > windowStart && position == windowStart + (windowSize / 2).min(2)

  def atBottomScrollingPoint(position: Int, filtered: List[Int]) =
    position == windowStart + windowSize - (windowSize / 2).max(2).min(3) &&
      !(windowStart + windowSize > filtered.length - 1)

  def visibleEntries(filtered: List[Int]): List[Int] =
    filtered.slice(windowStart, windowStart + windowSize)

  protected def computeWindowStartAfterSearch: Int =
    showing match
      case None => 0
      case Some((filtered, selected)) =>
        val position = filtered.indexOf(selected)
        val newWindowStart =
          (position - windowSize / 2).max(0).min(filtered.length - windowSize)

        newWindowStart

  def up: A =
    showing match
      case None => this
      case Some((filtered, selected)) =>
        val position = filtered.indexOf(selected)

        if atTopScrollingPoint(position) then scrollUp.changeSelection(-1)
        else if position == 0 then this // no scrolling beyond the top
        else changeSelection(-1)
    end match
  end up

  def down: A =
    showing match
      case None => this
      case Some((filtered, selected)) =>
        val position = filtered.indexOf(selected)

        if position == filtered.length - 1 then
          this // no scrolling beyond the bottom
        else if atBottomScrollingPoint(position, filtered)
        then scrollDown.changeSelection(+1)
        else changeSelection(+1)

        end if
    end match
  end down
end InfiniscrollableState
