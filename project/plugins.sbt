addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.5")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.0")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

addSbtPlugin("com.eed3si9n" % "sbt-doge" % "0.1.5")

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.0")

addSbtPlugin("com.novocode" % "sbt-ornate" % "0.3")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.3")

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.2")

addSbtPlugin("com.heroku" % "sbt-heroku" % "2.1.0") // Used by the example-documented project
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")


lazy val `sbt-assets` = RootProject(file("../sbt-assets"))

val build = project.in(file(".")).dependsOn(`sbt-assets`)
