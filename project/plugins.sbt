addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.2")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.32")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.2")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.3")
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.4.4")
addSbtPlugin("io.github.jonas" % "sbt-paradox-material-theme" % "0.6.0")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.2")

addSbtPlugin(
  "com.heroku" % "sbt-heroku" % "2.1.4"
) // Used by the example-documented project
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

addSbtPlugin("io.spray" % "sbt-boilerplate" % "0.6.1")

lazy val `sbt-assets` = RootProject(file("../sbt-assets"))

val build = project.in(file(".")).dependsOn(`sbt-assets`)

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
