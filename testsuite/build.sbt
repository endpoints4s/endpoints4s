import EndpointsSettings._
import LocalCrossProject._

val testsuite =
  crossProject.crossType(CrossType.Pure).in(file("testsuite"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-testsuite",
      libraryDependencies ++= Seq(
        "com.github.tomakehurst" % "wiremock" % "2.6.0",
        "org.scalatest" %% "scalatest" % "3.0.1",
        compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
      )
    )
    .dependsOnLocalCrossProjects("algebra", "algebra-circe")

val `testsuite-js` = testsuite.js

val `testsuite-jvm` = testsuite.jvm
