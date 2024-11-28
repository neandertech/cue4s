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

private[cue4s] case class InfiniscrollableState(
    showing: Option[(List[Int], Int)],
    windowStart: Int,
    windowSize: Int,
):
  def changeSelection(move: Int) =
    showing match
      case None => this // do nothing, no alternatives are showing
      case a @ Some((filtered, showing)) =>
        val position = filtered.indexOf(showing)

        val newSelected =
          (position + move).max(0).min(filtered.length - 1)

        copy(showing = a.map(_ => (filtered, filtered(newSelected))))

  def resetWindow() =
    showing match
      case None => this
      case Some((filtered, selected)) =>
        val position = filtered.indexOf(selected)
        val newWindowStart =
          (position - windowSize / 2).max(0).min(filtered.length - windowSize)

        copy(windowStart = newWindowStart)

  def scrollUp = copy(windowStart = (windowStart - 1).max(0))

  def scrollDown = copy(windowStart = windowStart + 1)

  def atTopScrollingPoint(position: Int) =
    position > windowStart && position == windowStart + (windowSize / 2).min(2)

  def atBottomScrollingPoint(position: Int, filtered: List[Int]) =
    position == windowStart + windowSize - (windowSize / 2).max(2).min(3) &&
      !(windowStart + windowSize > filtered.length - 1)

  def visibleEntries(filtered: List[Int]): List[Int] =
    filtered.slice(windowStart, windowStart + windowSize)

  def up =
    showing match
      case None => this
      case Some((filtered, selected)) =>
        val position = filtered.indexOf(selected)

        if atTopScrollingPoint(position) then scrollUp.changeSelection(-1)
        else if position == 0 then this // no scrolling beyond the top
        else changeSelection(-1)
    end match
  end up

  def down =
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
