package com.indoorvivants.proompts

object CharCollector:
  enum State:
    case Init, ESC_Started, CSI_Started
    case CSI_Collecting(bytes: List[Byte])

  def decode(curState: State, char: Int): (State, Next | Event) =
    def isCSIParameterByte(b: Int) =
      (b >= 0x30 && b <= 0x3f)

    def isCSIIntermediateByte(b: Int) =
      (b >= 0x20 && b <= 0x2f)

    def isCSIFinalByte(b: Int) =
      (b >= 0x40 && b <= 0x7e)

    def error(msg: String) =
      (curState, Next.Error(msg))

    def emit(event: Event) =
      (curState, event)

    def toInit(result: Next | Event) =
      (State.Init, result)

    curState match
      case State.Init =>
        char match
          case ANSI.ESC =>
            (State.ESC_Started, Next.Continue)
          case 10 =>
            emit(Event.Key(KeyEvent.ENTER))
          case 127 =>
            emit(Event.Key(KeyEvent.DELETE))
          case _ =>
            emit(Event.Char(char))

      case State.ESC_Started =>
        char match
          case '[' =>
            (State.CSI_Started, Next.Continue)
          case _ =>
            error(s"Unexpected symbol ${char} following an ESC sequence")

      case State.CSI_Started =>
        char match
          case 'A' => toInit(Event.Key(KeyEvent.UP))
          case 'B' => toInit(Event.Key(KeyEvent.DOWN))
          case 'C' => toInit(Event.Key(KeyEvent.RIGHT))
          case 'D' => toInit(Event.Key(KeyEvent.LEFT))

          case b
              if isCSIParameterByte(b) || isCSIIntermediateByte(
                b
              ) =>
            (State.CSI_Collecting(b.toByte :: Nil), Next.Continue)

      case State.CSI_Collecting(bytes) =>
        char match
          case b if isCSIFinalByte(b) =>
            toInit(Event.CSICode(bytes))
          case _ =>
            error(s"Unexpected byte ${char}, expected CSI final byte")

    end match
  end decode

end CharCollector
