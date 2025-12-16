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

/** A base trait for interactive terminal components. `Result` is the type of
  * value the component produces when terminated normally.
  *
  * @param terminal
  * @param out
  */
trait PromptFramework[Result](terminal: Terminal, out: Output)
    extends PromptFrameworkPlatform[Result]:
  self =>

  /** This type represents the internal state of the component.
    */
  type PromptState

  /** Type of events this component can handle - at the very minimum it must
    * handle TerminalEvent (key presses, initialisation, terminal resizing,
    * etc.). Best used with a union type that includes `TerminalEvent`
    *
    * Example:
    * {{{
    * enum MyEvent:
    *   case Ping, Pong
    *
    * type Event = TerminalEvent | MyEvent
    * }}}
    */
  type Event >: TerminalEvent

  /** When component is first initialised, it will start from this state
    *
    * @return
    *   initial state
    */
  def initialState: PromptState

  /** Main handler for terminal events and optionally user events
    * @param event,
    *   see [Event] above
    * @return
    *   a [PromptAction] indicating how the state and status of the prompt are
    *   to be modified
    */
  def handleEvent(event: Event): PromptAction

  /** Terminal rendering of the prompt based purely on its state and status.
    * Each list element must be a string NOT containing any newlines. The prompt
    * framework mechanism will use the result of this method to incrementally
    * update the visible state of the prompt.
    *
    * This function MUST be pure, as the results might be cached.
    *
    * @param state
    * @param status
    * @return
    */
  def renderState(state: PromptState, status: Status): List[String]

  /** Prompt-specific status */
  protected enum Status:
    /** All prompts start as [Init], and once it moves to any other status it
      * cannot transition back.
      */
    case Init

    /** [Running] status is where the prompt spends most of its time – reacting
      * to terminal events and re-rendering. The value might not be available if
      * validation returns `Left[PromptError]`. If the
      */
    case Running(err: Either[PromptError, Result])

    /** Prompts transition to [Finished] when the validation is successful and a
      * [Result] value can be obtained and returned to caller.
      *
      * This status if final – prompt cannot transition to any other status
      * after that.
      */
    case Finished(result: Result)

    /** This status indicates that prompt was canceled – e.g. via Ctrl+C or
      * manually sending a [TerminalEvent.Interrupt].
      *
      * This status is final – prompt cannot transition to any other status
      * after that.
      */
    case Canceled
  end Status

  /** Prompt action is the only way to communicate with the internally managed
    * state and status of the prompt.
    */
  protected enum PromptAction:
    /** Modify status or state, or both */
    case Update(
        status: Status => Status = identity,
        state: PromptState => PromptState = identity,
    )

    /** do nothing */
    case Continue

    /** Immediately terminate this prompt, returning an
      * [CompletionError.Interrupted] error to the caller
      */
    case Stop
  end PromptAction

  protected object PromptAction:
    def set(state: PromptState, status: Status) =
      PromptAction.Update(_ => status, _ => state)

    /** Set [Status] to the given value. State remains unchanged */
    def setStatus(f: Status): PromptAction = PromptAction.Update(_ => f)

    /** Set [State] to the given value. Status remains unchanged */
    def setState(f: PromptState): PromptAction =
      PromptAction.Update(state = _ => f)

    /** Update [State] using the given function. [Status] remains unchanged */
    def updateState(f: PromptState => PromptState) =
      PromptAction.Update(state = f)
  end PromptAction

  /** Construct a new [PromptFramework] for a different type, with the mapping
    * function supporting validation
    */
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

  private[cue4s] final val handler = new TerminalHandler[Result]:
    override def setupBackchannel(notif: Next[Result] => Unit): Unit =
      backchannel = Some(notif)
    override def apply(ev: TerminalEvent): Next[Result] =
      if ev == TerminalEvent.Init then printPrompt()
      manage(handleEvent(ev))
    end apply

  final def currentState(): PromptState = state.current
  final def currentStatus(): Status     = status.current

  /** This method allows communicating with the prompt directly, triggering the
    * event handling logic. Any valid event can be sent.
    *
    * @param ev
    */
  final def send(ev: Event) =
    backchannel.foreach(b => b(manage(handleEvent(ev))))

  private var backchannel: Option[Next[Result] => Unit] = None

  private def manage(a: PromptAction) =
    a match
      case PromptAction.Continue => Next.Continue
      case PromptAction.Stop     => Next.Stop

      case PromptAction.Update(statusF, stateF) =>
        stateTransition(stateF, statusF)
        val next: Next[Result] = currentStatus() match
          case Status.Finished(result) => Next.Done(result)
          case Status.Running(_)       => Next.Continue
          case Status.Canceled         => Next.Stop
          case Status.Init             => Next.Continue

        printPrompt()
        next

  end manage

  private def stateTransition(
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

  private def printPrompt() =
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
      case Status.Finished(_) => false
      case Status.Canceled    => false
      case _                  => true

  private var state = Transition(
    initialState,
  )
  private var status = Transition[Status](
    Status.Init,
  )
  private var rendering = Transition(
    renderState(currentState(), currentStatus()),
  )

end PromptFramework
