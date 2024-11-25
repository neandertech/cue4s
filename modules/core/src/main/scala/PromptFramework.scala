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

private[cue4s] trait PromptFramework(terminal: Terminal, out: Output):
  type PromptState
  type Result

  def initialState: PromptState
  def handleEvent(event: Event): Next[Result]
  def renderState(state: PromptState): List[String]
  def isRunning(state: PromptState): Boolean

  final val handler = new Handler[Result]:
    def apply(event: Event) = handleEvent(event)

  final def currentState(): PromptState = state.current

  final def stateTransition(s: PromptState => PromptState) =
    state = state.nextFn(s)
    rendering = rendering.nextFn: currentRendering =>
      val newRendering = renderState(state.current)
      if newRendering.length < currentRendering.length then
        newRendering ++ List.fill(
          currentRendering.length - newRendering.length
        )("")
      else newRendering
  end stateTransition

  final def printPrompt() =
    import terminal.*
    cursorHide()
    rendering.last match
      case None =>
        // initial print
        rendering.current.foreach(out.outLn)
        moveUp(rendering.current.length).moveHorizontalTo(0)
      case Some(value) =>
        def render =
          rendering.current
            .zip(value)
            .foreach: (line, oldLine) =>
              if line != oldLine then
                moveHorizontalTo(0).eraseEntireLine()
                out.out(line)
              moveDown(1)

        if isRunning(currentState()) then
          render
          moveUp(rendering.current.length).moveHorizontalTo(0)
        else // we are finished
          render
          // do not leave empty lines behind - move cursor up
          moveUp(rendering.current.reverse.takeWhile(_.isEmpty()).length)
            .moveHorizontalTo(0)
    end match

  end printPrompt

  private var state = Transition(
    initialState
  )
  private var rendering = Transition(renderState(currentState()))

end PromptFramework
