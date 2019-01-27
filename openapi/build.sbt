import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import EndpointsSettings._
import LocalCrossProject._

lazy val openapi =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("openapi"))
    .settings(
      publishSettings,
      `scala 2.11 to latest`,
      name := "endpoints-openapi"
    )
    .dependsOnLocalCrossProjects("json-schema-generic")
    .dependsOnLocalCrossProjectsWithScope(
      "algebra" -> "test->test;compile->compile",
      "json-schema" -> "test->test;compile->compile",
      "json-schema-circe" -> "test->test",
      "json-schema-playjson" -> "test->test"
    )

lazy val `openapi-js` = openapi.js
lazy val `openapi-jvm` = openapi.jvm

