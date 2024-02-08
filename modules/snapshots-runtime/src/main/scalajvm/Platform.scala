package proompts.snapshots

import java.nio.file.Paths
import java.io.FileWriter
import java.io.File

private[snapshots] trait Platform:
  extension (s: String)
    def resolve(segment: String): String =
      Paths.get(s).resolve(segment).toString()

    def fileWriteContents(contents: String): Unit =
      scala.util.Using(new FileWriter(new File(s))) { writer =>
        writer.write(contents)
      }

    def readFileContents(): Option[String] =
      val file = new File(s)
      Option.when(file.exists()):
        scala.io.Source
          .fromFile(file, "utf-8")
          .getLines()
          .mkString("\n")
  end extension
end Platform
