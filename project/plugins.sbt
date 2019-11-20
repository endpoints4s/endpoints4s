addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.29")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.8")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.3")

addSbtPlugin("com.novocode" % "sbt-ornate" % "0.6")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.2")

addSbtPlugin("com.heroku" % "sbt-heroku" % "2.1.2") // Used by the example-documented project
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

addSbtPlugin("io.spray" % "sbt-boilerplate" % "0.6.1")

lazy val `sbt-assets` = RootProject(file("../sbt-assets"))

val build = project.in(file(".")).dependsOn(`sbt-assets`)

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
