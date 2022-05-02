val sbtCrossProjectVersion = "1.2.0"

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.2")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
libraryDependencies += "org.scala-js" %% "scalajs-env-selenium" % "1.1.1"
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.8.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % sbtCrossProjectVersion)

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.4")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % sbtCrossProjectVersion)

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.12")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.1")
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.9.2")
addSbtPlugin("io.github.jonas" % "sbt-paradox-material-theme" % "0.6.0")

addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.5.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")

//addSbtPlugin(
//  "com.heroku" % "sbt-heroku" % "2.1.4"
//) // Used by the example-documented project
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.3")

addSbtPlugin("io.spray" % "sbt-boilerplate" % "0.6.1")

lazy val `sbt-assets` = RootProject(file("../sbt-assets"))

val build = project.in(file(".")).dependsOn(`sbt-assets`)

ThisBuild / ivyLoggingLevel := UpdateLogging.Quiet

addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "2.0.1")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.9")
