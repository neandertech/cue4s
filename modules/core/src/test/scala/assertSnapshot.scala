package proompts
import munit.internal.difflib.Diffs
import munit.Assertions

inline def assertSnapshot(inline name: String, contents: String) =
  Snapshots(name) match
    case None =>
      Snapshots.write(
        name,
        contents,
        Diffs.create(contents, "").createDiffOnlyReport()
      )

      Assertions.fail(
        s"No snapshot was found for $name, please run checkSnapshots command and accept a snapshot for this test"
      )

    case Some(value) =>
      val diff = Diffs.create(contents, value)
      if !diff.isEmpty then
        val diffReport = diff.createDiffOnlyReport()
        Snapshots.write(name, contents, diffReport)
        Assertions.assertNoDiff(contents, value)
