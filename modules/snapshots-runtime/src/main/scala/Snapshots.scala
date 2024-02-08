package proompts.snapshots

case class Snapshots(location: String, tmpLocation: String) extends Platform:
  inline def write(name: String, contents: String, diff: String): Unit =
    val tmpName     = name + "__snap.new"
    val tmpDiff     = name + "__snap.new.diff"
    val file        = location.resolve(name)
    val tmpFile     = tmpLocation.resolve(tmpName)
    val tmpFileDiff = tmpLocation.resolve(tmpDiff)

    val snapContents = 
      name + "\n" + file + "\n" + contents

    tmpFile.fileWriteContents(snapContents)
    tmpFileDiff.fileWriteContents(diff)
  end write

  inline def clear(name: String):Unit =
    val tmpName     = name + "__snap.new"
    val tmpDiff     = name + "__snap.new.diff"
    val tmpFile     = tmpLocation.resolve(tmpName)
    val tmpFileDiff = tmpLocation.resolve(tmpDiff)

    tmpFileDiff.delete()
    tmpFile.delete()


  inline def apply(inline name: String): Option[String] =
    location.resolve(name).readFileContents()

  end apply
end Snapshots
