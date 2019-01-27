import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import EndpointsSettings._
import LocalCrossProject._

val testsuite =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("testsuite"))
    .settings(
      publishSettings,
      `scala 2.11 to latest`,
      // testsuite project, so we don't need coverage here.
      coverageEnabled := false,
      name := "endpoints-testsuite",
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-generic" % circeVersion,
        "com.github.tomakehurst" % "wiremock" % "2.22.0",
        "org.scalatest" %%% "scalatest" % scalaTestVersion
      ),
      macroParadiseDependency
    )
    .dependsOnLocalCrossProjects("algebra", "algebra-circe", "algebra-playjson")

val `testsuite-js` = testsuite.js
val `testsuite-jvm` = testsuite.jvm
