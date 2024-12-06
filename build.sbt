import scala.scalanative.build.SourceLevelDebuggingConfig
import scala.io.StdIn
Global / excludeLintKeys += logManager
Global / excludeLintKeys += scalaJSUseMainModuleInitializer
Global / excludeLintKeys += scalaJSLinkerConfig

inThisBuild(
  List(
    organization           := "tech.neander",
    organizationName       := "Neandertech",
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    resolvers ++= Resolver.sonatypeOssRepos("releases"),
    homepage := Some(
      url("https://github.com/neandertech/cue4s"),
    ),
    startYear := Some(2023),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
    ),
    developers := List(
      Developer(
        "keynmol",
        "Anton Sviridov",
        "velbm@pm.me",
        url("https://blog.indoorvivants.com"),
      ),
    ),
    version := (if (!sys.env.contains("CI")) "dev" else version.value),
  ),
)

val Versions = new {
  val Scala3        = "3.3.4"
  val munit         = "1.0.2"
  val scalaVersions = Seq(Scala3)
  val fansi         = "0.5.0"
  val jna           = "5.14.0"
  val catsEffect    = "3.5.7"
  val osLib         = "0.11.3"
}

lazy val munitSettings = Seq(
  libraryDependencies += {
    "org.scalameta" %%% "munit" % Versions.munit % Test
  },
)

lazy val root = project
  .in(file("."))
  .aggregate(core.projectRefs*)
  .aggregate(catsEffect.projectRefs*)
  .aggregate(example.projectRefs*)
  .settings(noPublish)

lazy val core = projectMatrix
  .in(file("modules/core"))
  .defaultAxes(defaults*)
  .settings(
    name := "cue4s",
  )
  .settings(munitSettings)
  .jvmPlatform(Versions.scalaVersions)
  .jsPlatform(Versions.scalaVersions)
  .nativePlatform(Versions.scalaVersions)
  .settings(
    snapshotsPackageName := "cue4s",
    snapshotsIntegrations += SnapshotIntegration.MUnit,
    scalacOptions += "-Wunused:all",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    libraryDependencies += "com.lihaoyi" %%% "fansi" % Versions.fansi,
    libraryDependencies +=
      "net.java.dev.jna" % "jna" % Versions.jna,
    nativeConfig ~= (_.withIncrementalCompilation(true)),
  )
  .enablePlugins(SnapshotsPlugin)
  .settings(superMatrix)

lazy val catsEffect = projectMatrix
  .in(file("modules/cats-effect"))
  .defaultAxes(defaults*)
  .settings(
    name := "cue4s-cats-effect",
  )
  .dependsOn(core)
  .settings(munitSettings)
  .jvmPlatform(Versions.scalaVersions)
  .jsPlatform(Versions.scalaVersions)
  .settings(
    snapshotsPackageName := "cue4s.catseffect",
    snapshotsIntegrations += SnapshotIntegration.MUnit,
    scalacOptions += "-Wunused:all",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    libraryDependencies += "org.typelevel" %%% "cats-effect" % Versions.catsEffect,
    nativeConfig ~= (_.withIncrementalCompilation(true)),
  )
  .enablePlugins(SnapshotsPlugin)

lazy val example = projectMatrix
  .dependsOn(core)
  .in(file("modules/example"))
  .defaultAxes(defaults*)
  .enablePlugins(JavaAppPackaging, NativeImagePlugin)
  .settings(
    name := "example",
    noPublish,
  )
  .settings(munitSettings)
  .jvmPlatform(
    Versions.scalaVersions,
    settings = Seq(
      Compile / mainClass := Some("cue4s_example.sync"),
      nativeImageJvm      := "graalvm-java23",
      nativeImageVersion  := "23.0.0",
    ),
  )
  .jsPlatform(
    Versions.scalaVersions,
    settings = Seq(Compile / mainClass := Some("cue4s_example.future")),
  )
  .nativePlatform(
    Versions.scalaVersions,
    settings = Seq(Compile / mainClass := Some("cue4s_example.sync")),
  )
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    nativeConfig ~= (_.withIncrementalCompilation(true)
      .withSourceLevelDebuggingConfig(SourceLevelDebuggingConfig.enabled)),
  )
  .settings(superMatrix)

lazy val superMatrix = Seq((Compile / unmanagedSourceDirectories) ++= {
  val allCombos = List("js", "jvm", "native").combinations(2).toList
  val dis =
    virtualAxes.value.collectFirst { case p: VirtualAxis.PlatformAxis =>
      p.directorySuffix
    }.get

  allCombos
    .filter(_.contains(dis))
    .map { suff =>
      val suffixes = "scala" + suff.mkString("-", "-", "")

      (Compile / sourceDirectory).value / suffixes
    }
})

lazy val exampleCatsEffect = projectMatrix
  .dependsOn(core, catsEffect)
  .in(file("modules/example-catseffect"))
  .defaultAxes(defaults*)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "example",
    noPublish,
  )
  .settings(munitSettings)
  .jvmPlatform(Versions.scalaVersions)
  .jsPlatform(Versions.scalaVersions)
  .settings(
    scalacOptions += "-Wunused:all",
    scalaJSUseMainModuleInitializer := true,
    Compile / mainClass             := Some("example.catseffect.ioExample"),
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    nativeConfig ~= (_.withIncrementalCompilation(true)),
  )

lazy val e2e_fixture = projectMatrix
  .dependsOn(core)
  .in(file("modules/e2e-fixture"))
  .defaultAxes(defaults*)
  .enablePlugins(JavaAppPackaging, NativeImagePlugin)
  .settings(
    noPublish,
  )
  .settings(munitSettings)
  .jvmPlatform(
    Versions.scalaVersions,
    settings = Seq(
      nativeImageJvm     := "graalvm-java23",
      nativeImageVersion := "23.0.0",
      nativeImageOptions += "--install-exit-handlers",
    ),
  )
  .jsPlatform(
    Versions.scalaVersions,
  )
  .nativePlatform(
    Versions.scalaVersions,
  )
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    nativeConfig ~= (_.withIncrementalCompilation(true)
      .withSourceLevelDebuggingConfig(SourceLevelDebuggingConfig.enabled)),
  )
  .settings(superMatrix)

lazy val e2e = project
  .in(file("modules/e2e-tests"))
  .settings(
    Test / envVars := Map(
      "CUE4S_EXAMPLE_NATIVE" -> (e2e_fixture.native(
        true,
      ) / Compile / nativeLink).value.toString,
      "CUE4S_EXAMPLE_NATIVE_IMAGE" -> (e2e_fixture.jvm(
        true,
      ) / Compile / nativeImage).value.toString,
      "CUE4S_EXAMPLE_JVM" -> (e2e_fixture.jvm(
        true,
      ) / Compile / assembly).value.toString,
      "CUE4S_EXAMPLE_JS" -> (e2e_fixture.js(
        true,
      ) / Compile / fastOptJS).value.data.toString,
    ),
    munitSettings,
    scalaVersion                          := Versions.Scala3,
    Test / fork                           := true,
    libraryDependencies += "com.lihaoyi" %%% "os-lib" % Versions.osLib,
  )

lazy val docs =
  project
    .in(file("target/.docs-target"))
    .enablePlugins(MdocPlugin)
    .settings(scalaVersion := Versions.Scala3)
    .dependsOn(core.jvm(true), catsEffect.jvm(true))

val noPublish = Seq(
  publish / skip      := true,
  publishLocal / skip := true,
)

val defaults =
  Seq(VirtualAxis.scalaABIVersion(Versions.Scala3), VirtualAxis.jvm)

val scalafixRules = Seq(
  "OrganizeImports",
  "DisableSyntax",
  "LeakingImplicitClassVal",
  "NoValInForComprehension",
).mkString(" ")

val CICommands = Seq(
  "clean",
  "scalafixEnable",
  "compile",
  "test",
  "e2e/test",
  "checkDocs",
  "scalafmtCheckAll",
  "scalafmtSbtCheck",
  s"scalafix --check $scalafixRules",
  "headerCheck",
).mkString(";")

val PrepareCICommands = Seq(
  s"scalafix --rules $scalafixRules",
  "scalafmtAll",
  "scalafmtSbt",
  "headerCreate",
).mkString(";")

addCommandAlias("ci", CICommands)

addCommandAlias("preCI", PrepareCICommands)

addCommandAlias(
  "testSnapshots",
  """set Test/envVars += ("SNAPSHOTS_INTERACTIVE" -> "true"); test""",
)

addCommandAlias("checkDocs", "docs/mdoc --in README.md")

Global / onChangedBuildSource := ReloadOnSourceChanges
