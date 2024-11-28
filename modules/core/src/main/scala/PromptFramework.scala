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

private[cue4s] trait PromptFramework[Result](terminal: Terminal, out: Output):
  self =>
  type PromptState

  def initialState: PromptState
  def handleEvent(event: Event): PromptAction[Result]
  def renderState(state: PromptState, error: Option[PromptError]): List[String]
  def isRunning(state: PromptState): Boolean

  def result(state: PromptState): Either[PromptError, Result]

  final def mapValidated[Derived](
      f: Result => Either[PromptError, Derived],
  ): PromptFramework[Derived] =
    new PromptFramework[Derived](terminal, out):

      override def initialState: PromptState = self.initialState

      override def renderState(
          state: PromptState,
          error: Option[PromptError],
      ): List[String] =
        self.renderState(state, result(state).left.toOption)

      override def isRunning(state: PromptState): Boolean =
        self.isRunning(state)

      override def result(state: PromptState): Either[PromptError, Derived] =
        self.result(state).flatMap(f)

      override type PromptState = self.PromptState

      override def handleEvent(
          event: Event,
      ): PromptAction[Derived] =
        self.handleEvent(event) match
          case self.PromptAction.Submit(submit) =>
            self.result(currentState()) match
              case Left(value) => PromptAction.Continue
              case Right(value) =>
                val finish = submit(value)
                f(value) match
                  case Left(value) => PromptAction.Continue
                  case Right(value) =>
                    PromptAction.Submit(_ => finish)

          case self.PromptAction.Start    => PromptAction.Start
          case self.PromptAction.Stop     => PromptAction.Stop
          case self.PromptAction.Continue => PromptAction.Continue
          case self.PromptAction.Update(f) =>
            PromptAction.Update(f)
          case self.PromptAction.UpdateAndStop(f) =>
            PromptAction.UpdateAndStop(f)

  end mapValidated

  final val handler = new Handler[Result]:
    override def apply(ev: Event): Next[Result] =
      handleEvent(ev) match
        case PromptAction.Continue => Next.Continue
        case PromptAction.Stop     => Next.Stop
        case PromptAction.Start =>
          printPrompt()
          Next.Continue
        case PromptAction.UpdateAndStop(f) =>
          stateTransition(f)
          printPrompt()
          Next.Stop
        case PromptAction.Submit(finish) =>
          result(currentState()) match
            case Left(value) => Next.Continue
            case Right(value) =>
              stateTransition(finish(value))
              printPrompt()
              Next.Done(value)

        case PromptAction.Update(f) =>
          stateTransition(f)
          printPrompt()
          Next.Continue

  final def currentState(): PromptState = state.current

  final def stateTransition(s: PromptState => PromptState) =
    state = state.nextFn(s)
    rendering = rendering.next(
      renderState(state.current, result(state.current).left.toOption),
    )
  end stateTransition

  final def printPrompt() =
    import terminal.*
    cursorHide()
    rendering.last match
      case None =>
        // initial print
        rendering.current.foreach(out.outLn)
        moveUp(rendering.current.length).moveHorizontalTo(0)
      case Some(previousRendering) =>
        val paddingLength =
          (previousRendering.length - rendering.current.length).max(0)

        inline def pad(n: Int) = List.fill(n)("")

        val (current, previous) =
          if rendering.current.length > previousRendering.length then
            (
              rendering.current,
              previousRendering ++ pad(
                rendering.current.length - previousRendering.length,
              ),
            )
          else
            (
              rendering.current ++ pad(
                previousRendering.length - rendering.current.length,
              ),
              previousRendering,
            )

        out.logLn(
          current
            .zip(previous)
            .toString(),
        )

        out.logLn(paddingLength.toString)

        def render =
          current
            .zip(previous)
            .foreach: (line, oldLine) =>
              if line != oldLine then
                moveHorizontalTo(0).eraseEntireLine()
                out.out(line)
              moveDown(1)

        if isRunning(currentState()) then
          render
          moveUp(current.length).moveHorizontalTo(0)
        else // we are finished
          render
          // out.logLn(paddingLength)
          // do not leave empty lines behind - move cursor up
          moveUp(paddingLength).moveHorizontalTo(0)

        end if
    end match

  end printPrompt

  private var state = Transition(
    initialState,
  )
  private var rendering = Transition(
    renderState(currentState(), result(currentState()).left.toOption),
  )
  enum PromptAction[-Result]:
    case Submit(f: Result => PromptState => PromptState)
    case Update(f: PromptState => PromptState)
    case UpdateAndStop(f: PromptState => PromptState)
    case Continue
    case Stop
    case Start
end PromptFramework
