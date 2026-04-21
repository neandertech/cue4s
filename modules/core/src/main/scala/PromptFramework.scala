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
  * When extending this trait, you need to override PromptState type (which
  * should be an **immutable** state, such as case class, which will contain
  * everything required to render your prompt).
  *
  * ```scala
  * class MyPrompt(terminal: Terminal, out: Output)
  *     extends PromptFramework[String](terminal, out):
  *   override type PromptState = String
  *   override type Event       = TerminalEvent
  *
  *   override def initialState: PromptState = ""
  *
  *   override def extractResult(s: String) = Right(s)
  *
  *   override def handleEvent(event: Event): PromptAction = event match
  *     case TerminalEvent.Key(KeyEvent.ENTER) =>
  *       PromptAction.TrySubmit
  *     case TerminalEvent.Char(char) =>
  *       PromptAction.updateState(_ :+ char.toChar)
  *     case _ => PromptAction.Continue
  *
  *   override def renderState(
  *       state: PromptState,
  *       status: Status,
  *   ): List[String] =
  *     List(s"Current state: $state")
  * end MyPrompt
  * ```
  *
  * @param terminal
  * @param out
  */
trait PromptFramework[Result](terminal: Terminal, out: Output)
    extends PromptFrameworkPlatform[Result]:
  self =>

  /** This type represents the internal state of the component – it should be an
    * **immutable** state, such as case class, which will contain everything
    * required to render your prompt.
    */
  type PromptState

  /** Type of events this component can handle - at the very minimum it must
    * handle TerminalEvent (key presses, initialisation, terminal resizing,
    * etc.). Best used with a union type that includes `TerminalEvent`
    *
    * Example:
    * ```scala
    * enum MyEvent:
    *   case Ping, Pong
    *
    * type Event = TerminalEvent | MyEvent
    * ```
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
    * Each list element mu= ?st be a string NOT containing any newlines. The
    * prompt framework mechanism will use the result of this method to
    * incrementally update the visible state of the prompt.
    *
    * This function MUST be pure, as the results might be cached.
    *
    * @param state
    * @param status
    * @return
    *   list of lines to be printed to terminal
    */
  def renderState(state: PromptState, status: Status): List[String]

  def extractResult(state: PromptState): Either[PromptError, Result]

  /** Prompt-specific status */
  protected enum Status:
    /** [Running] status is where the prompt spends most of its time – reacting
      * to terminal events and re-rendering. The value might not be available if
      * validation returns `Left[PromptError]`
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
        state: PromptState => PromptState = identity,
    )

    /** do nothing */
    case Continue

    case Submit(result: Result)

    case TrySubmit

    /** Immediately terminate this prompt, returning an
      * [CompletionError.Interrupted] error to the caller
      */
    case Stop
  end PromptAction

  protected object PromptAction:
    /** Set [State] to the given value. Status remains unchanged */
    def setState(f: PromptState): PromptAction =
      PromptAction.Update(state = _ => f)

    /** Update [State] using the given function. [Status] remains unchanged */
    def updateState(f: PromptState => PromptState) =
      PromptAction.Update(state = f)

    def trySubmit: PromptAction = PromptAction.TrySubmit

    def submit(result: Result): PromptAction = PromptAction.Submit(result)
  end PromptAction

  /** Construct a new [PromptFramework] for a different type, with the mapping
    * function supporting validation
    */
  final def mapValidated[Derived](
      f: Result => Either[PromptError, Derived],
  ): PromptFramework[Derived] =
    val inner = self
    new PromptFramework[Derived](terminal, out):
      override type PromptState = inner.PromptState
      override type Event       = inner.Event

      private val outer = this

      override def initialState: PromptState = inner.initialState

      override val state =
        Transition.readOnly(inner.state)

      override val rendering = Transition.readOnly(inner.rendering)

      override val status = LoggedTransition(
        "derived status",
        Transition.base(
          Status.Running(outer.extractResult(outer.state.current)),
        ),
        out,
      )

      override def extractResult(
          state: PromptState,
      ): Either[PromptError, Derived] =
        inner.extractResult(state).flatMap(f)

      override def renderState(
          state: PromptState,
          status: Status,
      ): List[String] =
        inner.renderState(inner.currentState(), inner.currentStatus())

      private def statuses(
          state: PromptState,
      ): (inner.Status.Running, outer.Status.Running) =
        inner.extractResult(state) match
          case v @ Left(err) =>
            (inner.Status.Running(v), outer.Status.Running(Left(err)))
          case Right(innerValue) =>
            f(innerValue) match
              case v @ Left(err) =>
                (
                  inner.Status.Running(Left(err)),
                  outer.Status.Running(Left(err)),
                )
              case v @ Right(outerValue) =>
                (
                  inner.Status
                    .Running(Right(innerValue)),
                  outer.Status.Running(Right(outerValue)),
                )

      override def handleEvent(
          event: Event,
      ): PromptAction =
        import inner.PromptAction as InnerPromptAction
        val innerResult = inner.handleEvent(event)
        out.logLn(s"[derived] Inner result: $innerResult")
        innerResult match
          case InnerPromptAction.Continue => PromptAction.Continue
          case InnerPromptAction.Stop =>
            inner.stateTransition(status = _ => inner.Status.Canceled)
            PromptAction.Stop
          case InnerPromptAction.TrySubmit =>
            statuses(state.current) match
              case (
                    inner.Status.Running(Right(innerValue)),
                    outer.Status.Running(Right(outerValue)),
                  ) =>
                inner.stateTransition(status =
                  _ => inner.Status.Finished(innerValue),
                )
                PromptAction.Submit(outerValue)
              case (innerStatus, outerStatus) =>
                inner.stateTransition(status = _ => innerStatus)
                PromptAction.Continue

          case InnerPromptAction.Submit(result) =>
            statuses(state.current) match
              case (
                    inner.Status.Running(Right(innerValue)),
                    _,
                  ) =>
                inner.stateTransition(status =
                  _ => inner.Status.Finished(innerValue),
                )
                f(result) match
                  case Left(value)  => PromptAction.Continue
                  case Right(value) => PromptAction.Submit(value)
              case (innerStatus, _) =>
                inner.stateTransition(status = _ => innerStatus)
                PromptAction.Continue

          case InnerPromptAction.Update(stateChange) =>
            val tempState   = stateChange(inner.state.current)
            val innerStatus = statuses(tempState)._1

            inner.stateTransition(
              state = _ => tempState,
              status = _ => innerStatus,
            )

            PromptAction.Continue

        end match

      end handleEvent
    end new

  end mapValidated

  private[cue4s] final val handler = new TerminalHandler[Result]:
    override def setupBackchannel(notif: Next[Result] => Unit): Unit =
      backchannel = Some(notif)
    override def apply(ev: TerminalEvent): Next[Result] =
      out.logLn(s"Handling event $ev")
      ev match
        case TerminalEvent.Resized(rows, cols) =>
          this.synchronized:
            terminalSize.next(Some(cols))
          printPrompt()
        case _ =>

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
    out.logLn(s"Event sent directly to prompt: $ev")
    backchannel.foreach(b => b(manage(handleEvent(ev))))

  private var backchannel: Option[Next[Result] => Unit] = None

  private def manage(a: PromptAction) =
    out.logLn(s"Interpreting prompt action: $a")
    val next = a match
      case PromptAction.Continue => Next.Continue
      case PromptAction.Stop =>
        stateTransition(status = _ => Status.Canceled)
        Next.Stop

      case PromptAction.Update(stateF) =>
        stateTransition(
          stateF,
          _ => Status.Running(extractResult(state.current)),
        )

        status.current match
          case Status.Finished(result) => Next.Done(result)
          case Status.Running(_)       => Next.Continue
          case Status.Canceled         => Next.Stop

      case PromptAction.TrySubmit =>
        extractResult(state.current) match
          case Right(result) =>
            stateTransition(status = _ => Status.Finished(result))
            Next.Done(result)
          case _ => Next.Continue

      case PromptAction.Submit(value) =>
        stateTransition(status = _ => Status.Finished(value))
        out.logLn(s"Submitted value: $value, ${status.current}")
        Next.Done(value)

    if rendering.changed then printPrompt()

    next
  end manage

  private def stateTransition(
      state: PromptState => PromptState = identity,
      status: Status => Status = identity,
  ) =
    this.synchronized:
      val stateChanged  = this.state.nextFn(state)
      val statusChanged = this.status.nextFn(status)
      import Transition.Changed
      if stateChanged == Changed.Yes || statusChanged == Changed.Yes then
        this.rendering.next(
          renderState(this.state.current, this.status.current),
        )
  end stateTransition

  private def printPrompt() =
    terminal.cursorHide()
    rendering.last match
      case Some(value) if value.length > rendering.current.length =>
        out.logLn("Clearing because previous rendering was longer")
        terminal.withRestore:
          clear(value, terminalSize.current)
      case None =>
        // We don't use cursor save/restore here because after outputing empty lines,
        // the terminal might scroll and the saved cursor position would be incorrect
        out.logLn("Freeing up empty space for initial rendering")
        rendering.current.foreach(_ => out.outLn(""))
        terminal.moveUp(rendering.current.length).moveHorizontalTo(0)
        rendering.nextFn(identity)
      case _ =>
        out.logLn("Clearing just for current rendering")
        terminal.withRestore:
          clear(rendering.current, terminalSize.current)
    end match

    out.logLn(
      s"Rendering ${rendering.current} with ${terminalSize.current}",
    )
    currentStatus() match
      case _: Status.Finished | Status.Canceled =>
        rendering.current.foreach(l => out.outLn(l))
        terminal.cursorShow()
      case _ =>
        terminal.withRestore:
          rendering.current.foreach(l => out.outLn(l))
  end printPrompt

  def clear(lines: List[String], cols: Option[TerminalCols]) =
    cols match
      case Some(cols) =>
        val rows =
          lines
            .map(l =>
              1 + math.floor((l.length.toDouble - 1).max(0) / cols).toInt,
            )
            .sum
        out.logLn(s"Clearing ${rows} rows (with terminal size)")
        for _ <- 0 until rows do
          terminal.eraseEntireLine()
          terminal.moveDown(1)
      case None =>
        out.logLn(s"Clearing ${lines.length} rows (no terminal size)")
        for _ <- lines.indices do
          terminal.eraseEntireLine()
          terminal.moveDown(1)

  protected val state: Transition[self.PromptState] = LoggedTransition(
    "state",
    Transition.base(
      initialState,
    ),
    out,
  )

  protected val status = LoggedTransition(
    "status",
    Transition.base(
      Status.Running(extractResult(initialState)),
    ),
    out,
  )

  protected val rendering: Transition[List[String]] =
    LoggedTransition(
      "rendering",
      Transition.base(
        renderState(currentState(), currentStatus()),
      ),
      out,
    )

  private var terminalSize = Transition.base[Option[TerminalCols]](
    None,
  )

end PromptFramework
