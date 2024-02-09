package proompts.snapshots.sbtplugin

import sbt.Keys.*
import sbt.nio.Keys.*
import sbt.*
import scala.io.StdIn

object SnapshotsPlugin extends AutoPlugin {
  object autoImport {
    val snapshotsProjectIdentifier    = settingKey[String]("")
    val snapshotsPackageName          = settingKey[String]("")
    val snapshotsAddRuntimeDependency = settingKey[Boolean]("")
    val tag            = ConcurrentRestrictions.Tag("snapshots-check")
    val snapshotsCheck = taskKey[Unit]("")
  }

  import autoImport.*

  override def globalSettings: Seq[Setting[?]] = Seq(
    concurrentRestrictions += Tags.limit(tag, 1)
  )

  override def projectSettings: Seq[Setting[?]] =
    Seq(
      libraryDependencies ++= {
        if (snapshotsAddRuntimeDependency.value) {
          val cross = crossVersion.value match {
            case b: Binary => b.prefix + scalaBinaryVersion.value
            case _         => scalaBinaryVersion.value
          }

          Seq(
            "tech.neander" % s"snapshots-runtime_$cross" % BuildInfo.version
          )
        } else Seq.empty
      },
      snapshotsProjectIdentifier    := moduleName.value,
      snapshotsAddRuntimeDependency := true,
      snapshotsCheck := Def
        .task {
          val bold  = scala.Console.BOLD
          val reset = scala.Console.RESET
          val legend =
            s"${bold}a${reset} - accept, ${bold}s${reset} - skip\nYour choice: "
          val modified = IO
            .listFiles(
              (Test / managedResourceDirectories).value.head / "snapshots-tmp"
            )
            .toList

          if (modified.isEmpty) {
            System.err.println(
              s"No snapshots to check in [${snapshotsProjectIdentifier.value}]"
            )
          } else {

            modified
              .filter(_.getName.endsWith("__snap.new"))
              .foreach { f =>
                val diffFile = new File(f.toString() + ".diff")
                assert(diffFile.exists(), s"Diff file $diffFile not found")

                val diffContents = scala.io.Source
                  .fromFile(diffFile)
                  .getLines()
                  .mkString(System.lineSeparator())

                val snapshotName :: destination :: newContentsLines =
                  scala.io.Source.fromFile(f).getLines().toList

                println(
                  s"Project ID: ${bold}${snapshotsProjectIdentifier.value}${reset}"
                )
                println(
                  s"Name: ${scala.Console.BOLD}$snapshotName${scala.Console.RESET}"
                )
                println(
                  s"Path: ${scala.Console.BOLD}$destination${scala.Console.RESET}"
                )
                println(diffContents)

                println("\n\n")
                print(legend)

                val choice = StdIn.readLine().trim

                if (choice == "a") {
                  IO.writeLines(new File(destination), newContentsLines)
                  IO.delete(f)
                  IO.delete(diffFile)
                }

              }
          }

        }
        .tag(tag)
        .value,
      Test / sourceGenerators += Def.task {
        val name        = snapshotsProjectIdentifier.value
        val packageName = snapshotsPackageName.value

        val snapshotsDestination = (Test / resourceDirectory).value / name

        val sourceDest =
          (Test / managedSourceDirectories).value.head / "Snapshots.scala"

        val tmpDest =
          (Test / managedResourceDirectories).value.head / "snapshots-tmp"

        IO.write(
          sourceDest,
          SnapshotsGenerate(snapshotsDestination, tmpDest, packageName)
        )

        IO.createDirectory(snapshotsDestination)
        IO.createDirectory(tmpDest)

        Seq(sourceDest)
      }
    )

  def SnapshotsGenerate(path: File, tempPath: File, packageName: String) =
    s"""
     |package $packageName
     |object Snapshots extends proompts.snapshots.Snapshots(location = "$path", tmpLocation = "$tempPath")
      """.trim.stripMargin

}
