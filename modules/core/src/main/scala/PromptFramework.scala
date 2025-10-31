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

trait PromptFramework[Result](terminal: Terminal, out: Output):
  self =>
  type PromptState
  type Event >: TerminalEvent
  def initialState: PromptState
  def handleEvent(event: Event): PromptAction
  def renderState(state: PromptState, status: Status): List[String]

  final def mapValidated[Derived](
      f: Result => Either[PromptError, Derived],
  ): PromptFramework[Derived] =
    new PromptFramework[Derived](terminal, out):
      override type PromptState = self.PromptState
      override type Event       = self.Event

      override def initialState: PromptState = self.initialState

      override def renderState(
          state: PromptState,
          status: Status,
      ): List[String] =
        self.renderState(state, self.currentStatus())

      override def handleEvent(
          event: Event,
      ): PromptAction =
        self.handleEvent(event) match
          case self.PromptAction.Update(statusChange, stateChange) =>
            self.stateTransition(stateChange, statusChange)

            val refinedStatus =
              self.currentStatus() match
                case self.Status.Finished(result) =>
                  f(result) match
                    case Left(value)  => Status.Running(Left(value))
                    case Right(value) => Status.Finished(value)

                case self.Status.Canceled   => Status.Canceled
                case self.Status.Running(r) => Status.Running(r.flatMap(f))
                case self.Status.Init       => Status.Init

            // propagate information backwards...
            refinedStatus match
              case Status.Running(Left(err)) =>
                self.stateTransition(
                  identity,
                  _ => self.Status.Running(Left(err)),
                )
              case _ =>

            PromptAction.Update(_ => refinedStatus, stateChange)

          case self.PromptAction.Continue => PromptAction.Continue
          case self.PromptAction.Stop     => PromptAction.Stop

  end mapValidated

  // final def send(ev: Event): PromptAction =
  //   if ev == TerminalEvent.Init then printPrompt()
  //   handleEvent(ev) match
  //     case PromptAction.Continue => Next.Continue
  //     case PromptAction.Stop     => Next.Stop

  //     case PromptAction.Update(statusF, stateF) =>
  //       stateTransition(stateF, statusF)
  //       val next = currentStatus() match
  //         case Status.Finished(result) => Next.Done(result)
  //         case Status.Running(_)       => Next.Continue
  //         case Status.Canceled         => Next.Stop
  //         case Status.Init             => Next.Continue

  //       printPrompt()
  //       next
  //   end match
  // end send

  private var backchannel: Option[Next[Result] => Unit] = None

  final def send(ev: Event) =
    backchannel.foreach(b => b(manage(handleEvent(ev))))

  private def manage(a: PromptAction) = a match
    case PromptAction.Continue => Next.Continue
    case PromptAction.Stop     => Next.Stop

    case PromptAction.Update(statusF, stateF) =>
      stateTransition(stateF, statusF)
      val next = currentStatus() match
        case Status.Finished(result) => Next.Done(result)
        case Status.Running(_)       => Next.Continue
        case Status.Canceled         => Next.Stop
        case Status.Init             => Next.Continue

      printPrompt()
      next

  final val handler = new Handler[Result]:
    override def setupBackchannel(notif: Next[Result] => Unit): Unit = 
      backchannel = Some(notif)
    override def apply(ev: TerminalEvent): Next[Result] =
      if ev == TerminalEvent.Init then printPrompt()
      manage(handleEvent(ev))
    end apply

  final def currentState(): PromptState = state.current
  final def currentStatus(): Status     = status.current

  final def stateTransition(
      stateChange: PromptState => PromptState,
      statusChange: Status => Status,
  ) =
    this.synchronized:
      state = state.nextFn(stateChange)
      status = status.nextFn(statusChange)
      rendering = rendering.next(
        renderState(state.current, status.current),
      )
  end stateTransition

  final def printPrompt() =
    import terminal.*
    this.synchronized:
      if currentStatus() != Status.Canceled then cursorHide()
      rendering.last match
        case None =>
          // initial print
          rendering.current.foreach: line =>
            out.outLn(line)
            moveHorizontalTo(0)
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

          def render =
            current
              .zip(previous)
              .foreach: (line, oldLine) =>
                if line != oldLine then
                  moveHorizontalTo(0).eraseEntireLine()
                  out.out(line)
                moveDown(1)

          if isRunning(currentStatus()) then
            render
            moveUp(current.length).moveHorizontalTo(0)
          else // we are finished
            render
            // do not leave empty lines behind - move cursor up
            moveUp(paddingLength).moveHorizontalTo(0)

          end if
      end match

  end printPrompt

  private def isRunning(status: Status) =
    status match
      case Status.Finished(result) => false
      case Status.Canceled         => false
      case _                       => true

  private var state = Transition(
    initialState,
  )
  private var status = Transition[Status](
    Status.Init,
  )
  private var rendering = Transition(
    renderState(currentState(), currentStatus()),
  )

  protected enum Status:
    case Init
    case Running(err: Either[PromptError, Result])
    case Finished(result: Result)
    case Canceled

  protected enum PromptAction:
    case Update(
        status: Status => Status = identity,
        state: PromptState => PromptState = identity,
    )
    case Continue, Stop

  object PromptAction:
    def setStatus(f: Status): PromptAction = PromptAction.Update(_ => f)
    def setState(f: PromptState): PromptAction =
      PromptAction.Update(state = _ => f)
    def updateState(f: PromptState => PromptState) =
      PromptAction.Update(state = f)
  end PromptAction
end PromptFramework
