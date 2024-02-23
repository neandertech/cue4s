import scala.io.StdIn
Global / excludeLintKeys += logManager
Global / excludeLintKeys += scalaJSUseMainModuleInitializer
Global / excludeLintKeys += scalaJSLinkerConfig

inThisBuild(
  List(
    semanticdbEnabled          := true,
    semanticdbVersion          := scalafixSemanticdb.revision,
    scalafixScalaBinaryVersion := scalaBinaryVersion.value,
    organization               := "com.indoorvivants",
    organizationName           := "Anton Sviridov",
    resolvers ++= Resolver.sonatypeOssRepos("releases"),
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
  val Scala3        = "3.3.1"
  val munit         = "1.0.0-M11"
  val scalaVersions = Seq(Scala3)
}

// https://github.com/cb372/sbt-explicit-dependencies/issues/27
lazy val disableDependencyChecks = Seq(
  unusedCompileDependenciesTest     := {},
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
  .settings(
    snapshotsPackageName := "proompts",
    snapshotsIntegrations += SnapshotIntegration.MUnit,
    scalacOptions += "-Wunused:all",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    libraryDependencies += "com.lihaoyi" %%% "fansi" % "0.4.0",
    nativeConfig ~= (_.withIncrementalCompilation(true))
  )
  .enablePlugins(SnapshotsPlugin)

lazy val catsEffect = projectMatrix
  .in(file("modules/cats-effect"))
  .defaultAxes(defaults*)
  .settings(
    name := "cats-effect"
  )
  .dependsOn(core)
  .settings(munitSettings)
  .jvmPlatform(Versions.scalaVersions)
  .jsPlatform(Versions.scalaVersions, disableDependencyChecks)
  .nativePlatform(Versions.scalaVersions, disableDependencyChecks)
  .settings(
    snapshotsPackageName := "proompts.catseffect",
    snapshotsIntegrations += SnapshotIntegration.MUnit,
    scalacOptions += "-Wunused:all",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    libraryDependencies += "org.typelevel" %%% "cats-effect" % "3.5.3",
    nativeConfig ~= (_.withIncrementalCompilation(true))
  )
  .enablePlugins(SnapshotsPlugin)

lazy val example = projectMatrix
  .dependsOn(core, catsEffect)
  .in(file("modules/example"))
  .defaultAxes(defaults*)
  .settings(
    name := "example",
    noPublish
  )
  .settings(munitSettings)
  .jvmPlatform(Versions.scalaVersions)
  .jsPlatform(Versions.scalaVersions, disableDependencyChecks)
  .nativePlatform(Versions.scalaVersions, disableDependencyChecks)
  .settings(
    scalacOptions += "-Wunused:all",
    scalaJSUseMainModuleInitializer := true,
    mainClass := Some("example.io.ioExample"),
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    nativeConfig ~= (_.withIncrementalCompilation(true))
  )

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
  "unusedCompileDependenciesTest"
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
