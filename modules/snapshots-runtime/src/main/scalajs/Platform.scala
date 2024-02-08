package proompts.snapshots

import scala.scalajs.js.annotation.JSImport
import scalajs.js

trait Platform:
  extension (s: String)
    def resolve(segment: String): String =
      s + "/" + segment

    def fileWriteContents(contents: String): Unit =
      FS.writeFileSync(s, contents)

    def delete(): Unit = 
      FS.rmSync(s, js.Dynamic.literal(force = true))


    def readFileContents(): Option[String] =
      val exists =
        FS.statSync(
          s,
          js.Dynamic.literal(throwIfNoEntry = false)
        ) != js.undefined
      Option.when(exists):
        FS.readFileSync(s, js.Dynamic.literal(encoding = "utf8"))
    end readFileContents
  end extension

end Platform

@js.native
private[snapshots] trait FS extends js.Object:
  def readFileSync(path: String, options: String | js.Object = ""): String =
    js.native
  def rmSync(path: String, options: js.Object = js.Object()): String =
    js.native
  def writeFileSync(
      path: String,
      contents: String,
      options: String = ""
  ): Unit = js.native
  def statSync(path: String, options: js.Any): js.Any = js.native
end FS

@js.native
@JSImport("node:fs", JSImport.Namespace)
private[snapshots] object FS extends FS
