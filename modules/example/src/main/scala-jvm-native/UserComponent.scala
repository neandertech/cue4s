package cue4s_test

import cue4s.*
import cue4s.KeyEvent
import java.util.concurrent.atomic.AtomicBoolean
import java.time.LocalDate
import java.time.LocalTime

@main def tictac =
  val terminal = Terminal.ansi(Output.Std)
  val input    = InputProvider(terminal)
  val tic      = TicTacToe(terminal, Output.Std)

  val finish = AtomicBoolean()

  val t = new Thread:
    override def run(): Unit =
      var flip = false
      while !finish.get() do
        if flip then tic.send(TimerEvent.ShowTime)
        else tic.send(TimerEvent.ShowDate)

        flip = !flip

        Thread.sleep(2000)

  t.start()
  val result = input.evaluate(tic.handler)
  finish.set(true)
  t.join()

  println(result)
end tictac

class TicTacToe(terminal: Terminal, out: Output)
    extends PromptFramework[Outcome](terminal, out):
  override type PromptState = State
  override type Event       = TerminalEvent | TimerEvent

  override def initialState: PromptState = State.init

  override def handleEvent(event: TerminalEvent | TimerEvent): PromptAction =
    event match
      case t: TimerEvent    => handleTimerEvent(t)
      case t: TerminalEvent => handleTerminalEvent(t)

  private def handleTimerEvent(t: TimerEvent): PromptAction =
    t match
      case TimerEvent.ShowDate =>
        PromptAction.updateState(
          _.copy(flash = Some("Current date is " + LocalDate.now())),
        )
      case TimerEvent.ShowTime =>
        PromptAction.updateState(
          _.copy(flash = Some("Current time is " + LocalTime.now())),
        )

  private def handleTerminalEvent(t: TerminalEvent): PromptAction =
    t match
      case TerminalEvent.Init => PromptAction.Continue
      case TerminalEvent.Key(which) =>
        which match
          case KeyEvent.UP    => PromptAction.Update(state = _.shiftRow(-1))
          case KeyEvent.DOWN  => PromptAction.Update(state = _.shiftRow(+1))
          case KeyEvent.LEFT  => PromptAction.Update(state = _.shiftCol(-1))
          case KeyEvent.RIGHT => PromptAction.Update(state = _.shiftCol(+1))
          case KeyEvent.TAB   => checkWin
          case _              => PromptAction.Continue

      case TerminalEvent.Char(' ') => checkWin

      case TerminalEvent.Interrupt =>
        PromptAction.setStatus(Status.Canceled)

      case _ => PromptAction.Continue

  private def checkWin =
    val cur      = currentState()
    val newState = cur.toggle
    val newStatus = newState.finished match
      case Some(value) =>
        Status.Finished(value)
      case None =>
        currentStatus()

    PromptAction.Update(status = _ => newStatus, state = _ => newState)
  end checkWin

  override def renderState(state: State, status: Status): List[String] =
    val builder = List.newBuilder[String]
    import Turn.*
    import state.*

    state.finished match
      case Some(Outcome.Draw) =>
        builder += "It's a draw!"
      case Some(Outcome.Winner(Noughts)) =>
        builder += "Noughts have won"
      case Some(Outcome.Winner(Crosses)) =>
        builder += "Crosses have won"
      case None =>
        builder += "Arrows to move, Space/Tab to place your thing"

        builder +=
          (turn match
            case Noughts => s"Noughts (${Noughts.render})'s go"
            case Crosses => s"Crosses (${Crosses.render})'s go"
          )
    end match

    state.flash.foreach(builder += _)

    if debug then builder += state.toString()

    builder += "-" * (3 * 3 + 1 * 4)

    board.zipWithIndex.foreach: (row, i) =>
      builder += row.zipWithIndex
        .map: (row, j) =>
          val str = row.map(_.render).getOrElse(" ")
          if selected == (i, j) then fansi.Back.Cyan(fansi.Color.Black(str))
          else str
        .mkString("| ", " | ", " |")
      builder += "-" * (3 * 3 + 1 * 4)

    builder += ""

    builder.result()
  end renderState

end TicTacToe

enum TimerEvent:
  case ShowDate, ShowTime

enum Outcome:
  case Winner(turn: Turn)
  case Draw

enum Turn:
  case Noughts, Crosses

  def render = this match
    case Noughts => "O"
    case Crosses => "X"

  def toggle = this match
    case Noughts => Crosses
    case Crosses => Noughts
end Turn

import Turn.*

case class State(
    board: Vector[Vector[Option[Turn]]],
    turn: Turn,
    selected: (Int, Int),
    flash: Option[String],
    debug: Boolean = false,
):
  def cursorRow    = selected._1
  def cursorColumn = selected._2

  def toggle =
    if board(cursorRow)(cursorColumn).isEmpty then
      copy(
        board = board.updated(
          cursorRow,
          board(cursorRow).updated(cursorColumn, Some(turn)),
        ),
        turn = turn.toggle,
      )
    else this

  def shiftCol(i: 1 | -1) =
    copy(selected = (cursorRow, (cursorColumn + i).max(0).min(2)))

  def shiftRow(i: 1 | -1) =
    copy(selected = ((cursorRow + i).max(0).min(2), cursorColumn))

  def finished: Option[Outcome] =
    def rows(turn: Turn) =
      board.exists(_.distinct == Vector(Some(turn)))

    def cols(turn: Turn) =
      (0 until 3).exists(col => board.forall(_(col) == Some(turn)))

    def diag(turn: Turn) =
      (0 until 3).forall(i => board(i)(i) == Some(turn))

    def antidiag(turn: Turn) =
      (0 until 3).forall(i => board(i)(2 - i) == Some(turn))

    def check(turn: Turn) =
      Option.when(rows(turn) || cols(turn) || diag(turn) || antidiag(turn))(
        Outcome.Winner(turn),
      )

    check(Noughts) orElse
      check(Crosses) orElse
      Option.when(board.forall(_.forall(_.nonEmpty)))(Outcome.Draw)
  end finished
end State

object State:
  def init =
    new State(Vector.fill(3)(Vector.fill(3)(None)), Noughts, (0, 0), None)
