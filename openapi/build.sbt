import EndpointsSettings._
import LocalCrossProject._

lazy val `json-schemaJS` = LocalProject("json-schemaJS")
lazy val `json-schemaJVM` = LocalProject("json-schemaJVM")

lazy val openapi =
  crossProject.crossType(CrossType.Pure).in(file("openapi"))
    .settings(publishSettings ++ `scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-openapi",
      libraryDependencies += "io.circe" %%% "circe-core" % circeVersion
    )
    .jsConfigure(_.dependsOn(LocalProject("json-schemaJS") % "test->test;compile->compile"))
    .jvmConfigure(_.dependsOn(LocalProject("json-schemaJVM") % "test->test;compile->compile"))
    .dependsOnLocalCrossProjects("algebra")

lazy val `openapi-js` = openapi.js
lazy val `openapi-jvm` = openapi.jvm

