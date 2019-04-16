import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import EndpointsSettings._
import LocalCrossProject._

val testsuite =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("testsuite"))
    .settings(
      publishSettings,
      `scala 2.11 to 2.12`,
      // testsuite project, so we don't need coverage here.
      coverageEnabled := false,
      name := "endpoints-testsuite",
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-generic" % circeVersion,
        "com.github.tomakehurst" % "wiremock" % "2.23.2",
        "org.scalatest" %%% "scalatest" % scalaTestVersion,
        compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
      )
    )
    .dependsOnLocalCrossProjects("algebra", "algebra-circe", "algebra-playjson")

val `testsuite-js` = testsuite.js

val `testsuite-jvm` = testsuite.jvm
