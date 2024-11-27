package cue4s

import cue4s.*
import com.indoorvivants.snapshots.munit_integration.*
import scala.util.boundary

trait TerminalTests extends MunitSnapshotsIntegration:
  self: munit.FunSuite =>

  case object LogTag extends munit.Tag("logtest")

  def chars(str: String): List[Event] =
    str.map(Event.Char(_)).toList

  def list(args: (KeyEvent | Event | List[Event])*) =
    flatten(args.toList)

  def terminalTestComplete[R](
      name: munit.TestOptions
  )(prompt: PromptChain[R], events: List[Event | List[Event]], expected: R)(
      implicit loc: munit.Location
  ): Unit =
    test(name) {
      val Result(result, snapshot, processed) =
        runChain(flatten(events))(
          prompt
        )

      assertEquals(
        processed,
        flatten(events),
        s"Redundant events in tests (prompt completed before processing all of them): ${processed}"
      )

      assertSnapshot(name.name, snapshot)
      assertEquals(result, expected)
    }

  def flatten(evs: List[KeyEvent | Event | List[Event]]): List[Event] =
    evs.flatMap:
      case ev: Event    => List(ev)
      case ev: KeyEvent => List(Event.Key(ev))
      case ev: List[?]  => ev

  def terminalTestComplete[R](
      name: munit.TestOptions
  )(prompt: Prompt[R], events: List[Event], expected: R, log: Boolean = false)(
      implicit loc: munit.Location
  ): Unit =
    test(name) {
      val Result(result, snapshot, processed) =
        runToCompletion(events, log)(
          prompt
        )

      assertEquals(
        processed,
        events,
        s"Redundant events in tests (prompt completed before processing all of them): ${processed}"
      )

      assertSnapshot(name.name, snapshot)
      assertEquals(result, expected)
    }

  def terminalTest[R](
      name: munit.TestOptions
  )(
      prompt: Prompt[R],
      events: List[Event],
      expected: Next[R],
      log: Boolean = false
  )(implicit
      loc: munit.Location
  ): Unit =
    test(name) {
      val Result(result, snapshot, processed) =
        run(events, log)(
          prompt
        )

      assertSnapshot(name.name, snapshot)
      assertEquals(result, expected)
    }

  case class Result[T](value: T, snapshot: String, eventsProcessed: List[Event])

  def run[T](events: Seq[Event], log: Boolean = false)(
      prompt: Prompt[T]
  ): Result[Next[T]] =
    val sb = new java.lang.StringBuilder
    val logger: String => Unit =
      if log then s => sb.append(s + "\n") else _ => ()
    val term      = TracingTerminal(Output.Delegate(_ => (), logger))
    val capturing = Output.Delegate(term.writer, s => sb.append(s + "\n"))

    val handler =
      prompt.handler(term, capturing, colors = false)

    var result          = Option.empty[Next[T]]
    val eventsProcessed = List.newBuilder[Event]
    boundary:
      events.foreach: event =>
        sb.append(event.toString() + "\n")
        result = Some(handler(event))
        sb.append(term.getPretty() + "\n")

        eventsProcessed += event

        result match
          case Some(Next.Done(_)) => boundary.break()
          case _                  =>

    Result(
      result.getOrElse(fail("No events were processed")),
      sb.toString(),
      eventsProcessed.result()
    )
  end run

  def runToCompletion[T](events: Seq[Event], log: Boolean = false)(
      prompt: Prompt[T]
  ): Result[T] =
    val Result(result, sb, processed) = run(events, log)(prompt)

    result match
      case Next.Done(value) => Result(value, sb, processed)
      case _ => fail(s"Prompt wasn't evaluated to completion: $result")
  end runToCompletion

  def runChain[T](events: Seq[Event], log: Boolean = false)(
      prompt: PromptChain[T]
  ): Result[T] =
    val totalLog     = new java.lang.StringBuilder
    var eventsOffset = 0
    val evaluator: [t] => (p: Prompt[t]) => Completion[t] = [t] =>
      (p: Prompt[t]) =>
        val Result(res, sb, processed) =
          runToCompletion(events.drop(eventsOffset), log)(p)
        eventsOffset += processed.length
        totalLog.append(sb + "\n")
        Completion.Finished[t](res)

    prompt.run(evaluator) match
      case Completion.Finished(value) =>
        Result(value, totalLog.toString(), events.take(eventsOffset).toList)

      case Completion.Fail(error) =>
        fail("Prompt chain didn't finish successfully", error)

  end runChain

end TerminalTests
