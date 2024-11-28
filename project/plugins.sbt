addSbtPlugin("com.github.sbt" % "sbt-ci-release"    % "1.5.12")
addSbtPlugin("com.eed3si9n"   % "sbt-projectmatrix" % "0.9.1")

// Code quality
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"  % "2.5.2")
addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix"  % "0.11.1")
addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"    % "5.10.0")

// Compiled documentation
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.5.1")

// Scala.js and Scala Native
addSbtPlugin("org.scala-js"     % "sbt-scalajs"      % "1.17.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.6")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")

addSbtPlugin("com.indoorvivants.snapshots" % "sbt-snapshots" % "0.0.7")
