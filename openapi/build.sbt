import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import EndpointsSettings._
import LocalCrossProject._

lazy val openapi =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("openapi"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`, // We don’t support 2.11 because our tests have a dependency on circe
      name := "endpoints-openapi",
      (Compile / boilerplateSource) := (Compile / baseDirectory).value / ".." / "src" / "main" / "boilerplate",
      libraryDependencies += "com.lihaoyi" %%% "ujson" % "0.8.0"
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .dependsOnLocalCrossProjects("json-schema-generic")
    .dependsOnLocalCrossProjectsWithScope(
      "algebra" -> "test->test;compile->compile",
      "json-schema" -> "test->test;compile->compile",
      "json-schema-circe" -> "test->test",
      "json-schema-playjson" -> "test->test"
    )

lazy val `openapi-js` = openapi.js
lazy val `openapi-jvm` = openapi.jvm

