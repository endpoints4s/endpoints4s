addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.8")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")

lazy val `sbt-assets` = RootProject(file("../sbt-assets"))

val build = project.in(file(".")).dependsOn(`sbt-assets`)
