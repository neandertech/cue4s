addSbtPlugin("com.github.sbt" % "sbt-ci-release"    % "1.5.12")
addSbtPlugin("com.eed3si9n"   % "sbt-projectmatrix" % "0.9.1")

// Code quality
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"    % "0.1.22")
addSbtPlugin("ch.epfl.scala"             % "sbt-missinglink" % "0.3.6")
addSbtPlugin("com.github.cb372"  % "sbt-explicit-dependencies" % "0.2.16")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"              % "2.5.2")
addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix"              % "0.10.4")
addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo"             % "0.11.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"                % "5.9.0")

// Compiled documentation
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.3.7")

// Scala.js and Scala Native
addSbtPlugin("org.scala-js"     % "sbt-scalajs"      % "1.13.2")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.15")
