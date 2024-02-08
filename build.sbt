import scala.io.StdIn
Global / excludeLintKeys += logManager
Global / excludeLintKeys += scalaJSUseMainModuleInitializer
Global / excludeLintKeys += scalaJSLinkerConfig

inThisBuild(
  List(
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % Versions.organizeImports,
    semanticdbEnabled          := true,
    semanticdbVersion          := scalafixSemanticdb.revision,
    scalafixScalaBinaryVersion := scalaBinaryVersion.value,
    organization               := "com.indoorvivants",
    organizationName           := "Anton Sviridov",
    homepage := Some(
      url("https://github.com/neandertech/proompts")
    ),
    startYear := Some(2023),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "keynmol",
        "Anton Sviridov",
        "keynmol@gmail.com",
        url("https://blog.indoorvivants.com")
      )
    )
  )
)

val Versions = new {
  val Scala3          = "3.3.1"
  val munit           = "1.0.0-M7"
  val organizeImports = "0.6.0"
  val scalaVersions   = Seq(Scala3)
}

// https://github.com/cb372/sbt-explicit-dependencies/issues/27
lazy val disableDependencyChecks = Seq(
  unusedCompileDependenciesTest     := {},
  missinglinkCheck                  := {},
  undeclaredCompileDependenciesTest := {}
)

lazy val munitSettings = Seq(
  libraryDependencies += {
    "org.scalameta" %%% "munit" % Versions.munit % Test
  }
)

lazy val root = project
  .in(file("."))
  .aggregate(core.projectRefs*)
  .aggregate(docs.projectRefs*)
  .settings(noPublish)

lazy val core = projectMatrix
  .in(file("modules/core"))
  .defaultAxes(defaults*)
  .settings(
    name := "core"
  )
  .settings(munitSettings)
  .jvmPlatform(Versions.scalaVersions)
  .jsPlatform(Versions.scalaVersions, disableDependencyChecks)
  .nativePlatform(Versions.scalaVersions, disableDependencyChecks)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := "com.indoorvivants.library.internal",
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      scalaVersion,
      scalaBinaryVersion
    ),
    scalacOptions += "-Wunused:all",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    libraryDependencies += "com.lihaoyi" %%% "fansi" % "0.4.0",
    nativeConfig ~= (_.withIncrementalCompilation(true)),
    withSnapshotTesting
  )

val checkSnapshots = taskKey[Unit]("")

val withSnapshotTesting = Seq(
  checkSnapshots := {
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
      System.err.println("No snapshots to check")
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

  },
  Test / sourceGenerators += Def.task {
    val platformSuffix =
      virtualAxes.value.collectFirst { case p: VirtualAxis.PlatformAxis =>
        p
      }.get

    val isNative = platformSuffix.value == "jvm"
    val isJS     = platformSuffix.value == "js"
    val isJVM    = !isNative && !isJS

    val name = moduleName.value + "-" + platformSuffix.value

    val snapshotsDestination = (Test / resourceDirectory).value / name

    val sourceDest =
      (Test / managedSourceDirectories).value.head / "Snapshots.scala"

    val tmpDest =
      (Test / managedResourceDirectories).value.head / "snapshots-tmp"

    IO.write(sourceDest, SnapshotsGenerate(snapshotsDestination, tmpDest))

    IO.createDirectory(snapshotsDestination)
    IO.createDirectory(tmpDest)

    Seq(sourceDest)
  }
)

def SnapshotsGenerate(path: File, tempPath: File) =
  """
 |package proompts
 |import scala.quoted.* // imports Quotes, Expr
 |object Snapshots:
 |  inline def location(): String = "PATH"
 |  inline def tmpLocation(): String = "TEMP_PATH"
 |  inline def write(name: String, contents: String, diff: String): Unit = 
 |    import java.io.FileWriter
 |    val tmpName = name + "__snap.new"
 |    val tmpDiff = name + "__snap.new.diff"
 |    val file = java.nio.file.Paths.get(location()).resolve(name)
 |    val tmpFile = java.nio.file.Paths.get(tmpLocation()).resolve(tmpName).toFile
 |    val tmpFileDiff = java.nio.file.Paths.get(tmpLocation()).resolve(tmpDiff).toFile
 |    scala.util.Using(new FileWriter(tmpFile)) { writer => 
 |      writer.write(name + "\n")
 |      writer.write(file.toString + "\n")
 |      writer.write(contents)
 |    }
 |    scala.util.Using(new FileWriter(tmpFileDiff)) { writer => 
 |      writer.write(diff)
 |    }
 |  inline def apply(inline name: String): Option[String] =
 |    ${ applyImpl('name) }
 |  private def applyImpl(x: Expr[String])(using
 |      Quotes
 |  ): Expr[Option[String]] =
 |    val path = java.nio.file.Paths.get(location()).resolve(x.valueOrAbort)
 |    if path.toFile.exists() then 
 |      val str = scala.io.Source.fromFile(path.toFile, "utf-8").getLines().mkString(System.lineSeparator())
 |      Expr(Some(str))
 |    else Expr(None)
 |end Snapshots
  """.trim.stripMargin
    .replace("TEMP_PATH", tempPath.toPath().toAbsolutePath().toString)
    .replace("PATH", path.toPath().toAbsolutePath().toString)

lazy val docs = projectMatrix
  .in(file("myproject-docs"))
  .dependsOn(core)
  .defaultAxes(defaults*)
  .settings(
    mdocVariables := Map(
      "VERSION" -> version.value
    )
  )
  .settings(disableDependencyChecks)
  .jvmPlatform(Versions.scalaVersions)
  .enablePlugins(MdocPlugin)
  .settings(noPublish)

val noPublish = Seq(
  publish / skip      := true,
  publishLocal / skip := true
)

val defaults =
  Seq(VirtualAxis.scalaABIVersion(Versions.Scala3), VirtualAxis.jvm)

val scalafixRules = Seq(
  "OrganizeImports",
  "DisableSyntax",
  "LeakingImplicitClassVal",
  "NoValInForComprehension"
).mkString(" ")

val CICommands = Seq(
  "clean",
  "compile",
  "test",
  "docs/mdoc",
  "scalafmtCheckAll",
  "scalafmtSbtCheck",
  s"scalafix --check $scalafixRules",
  "headerCheck",
  "undeclaredCompileDependenciesTest",
  "unusedCompileDependenciesTest",
  "missinglinkCheck"
).mkString(";")

val PrepareCICommands = Seq(
  s"scalafix --rules $scalafixRules",
  "scalafmtAll",
  "scalafmtSbt",
  "headerCreate",
  "undeclaredCompileDependenciesTest"
).mkString(";")

addCommandAlias("ci", CICommands)

addCommandAlias("preCI", PrepareCICommands)

addCommandAlias(
  "testSnapshots",
  """set Test/envVars += ("SNAPSHOTS_INTERACTIVE" -> "true"); test"""
)

Global / onChangedBuildSource := ReloadOnSourceChanges
