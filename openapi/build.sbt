import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import EndpointsSettings._
import LocalCrossProject._

lazy val openapi =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("openapi"))
    .settings(
      version := "2.0.0",
      versionPolicyIntention := Compatibility.None,
      publishSettings,
      `scala 2.12 to dotty`,
      name := "openapi",
      (Compile / boilerplateSource) := (Compile / baseDirectory).value / ".." / "src" / "main" / "boilerplate",
      libraryDependencies += ("com.lihaoyi" %%% "ujson" % ujsonVersion).withDottyCompat(scalaVersion.value)
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .dependsOnLocalCrossProjectsWithScope(
      "algebra" -> "test->test;compile->compile",
      "json-schema" -> "test->test;compile->compile",
      "json-schema-generic" -> "test->test"
    )
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))

lazy val `openapi-js` = openapi.js
lazy val `openapi-jvm` = openapi.jvm
