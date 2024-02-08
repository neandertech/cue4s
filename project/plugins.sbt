addSbtPlugin("com.github.sbt" % "sbt-ci-release"    % "1.5.12")
addSbtPlugin("com.eed3si9n"   % "sbt-projectmatrix" % "0.9.1")

// Code quality
//addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"    % "0.4.2")
addSbtPlugin("ch.epfl.scala"     % "sbt-missinglink"           % "0.3.6")
addSbtPlugin("com.github.cb372"  % "sbt-explicit-dependencies" % "0.3.1")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"              % "2.5.2")
addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix"              % "0.11.1")
addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo"             % "0.11.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"                % "5.10.0")

// Compiled documentation
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.5.1")

// Scala.js and Scala Native
addSbtPlugin("org.scala-js"     % "sbt-scalajs"      % "1.14.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.17")

libraryDependencies ++= List(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)
Compile / unmanagedSourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile /
    "modules" / "snapshots-sbt-plugin" / "src" / "main" / "scala"

Compile / sourceGenerators += Def.task {
  val tmpDest =
    (Compile / managedResourceDirectories).value.head / "BuildInfo.scala"

  IO.write(
    tmpDest,
    "package proompts.snapshots.sbtplugin\nobject BuildInfo {def version: String = \"dev\"}"
  )

  Seq(tmpDest)
}
