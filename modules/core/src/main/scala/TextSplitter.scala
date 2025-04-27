package cue4s

import scala.collection.mutable.ListBuffer

object TextSplitter:

  def split(s: String, max: Int): List[String] =

    val lines       = List.newBuilder[String]
    val currentLine = ListBuffer.empty[String]

    val words = s.split(" ")

    inline def recordLine() =
      if currentLine.nonEmpty then
        lines += currentLine.mkString(" ")
        currentLine.clear()

    var curWidth = 0
    words.foreach: word =>
      if curWidth < max then
        currentLine += word
        curWidth += word.length + (if currentLine.isEmpty then 0 else 1)
      else
        recordLine()
        currentLine += word
        curWidth = word.length
      end if

    recordLine()

    lines.result()

  end split

end TextSplitter
