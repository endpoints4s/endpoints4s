import EndpointsSettings._
import LocalCrossProject._

val `json-schema` =
  crossProject.crossType(CrossType.Pure).in(file("json-schema"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-algebra-json-schema",
      addScalaTestCrossDependency
    )

val `json-schema-js` = `json-schema`.js
val `json-schema-jvm` = `json-schema`.jvm

lazy val `json-schema-generic` =
  crossProject.crossType(CrossType.Pure).in(file("json-schema-generic"))
    .settings(publishSettings ++ `scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-json-schema-generic",
      libraryDependencies += "com.chuusai" %%% "shapeless" % "2.3.2",
      addScalaTestCrossDependency
    )
    .dependsOnLocalCrossProjects("json-schema")
    .dependsOnLocalCrossProjectsWithScope("openapi" -> "test")

lazy val `json-schema-generic-js` = `json-schema-generic`.js
lazy val `json-schema-generic-jvm` = `json-schema-generic`.jvm

lazy val `json-schema-circe` =
  crossProject.crossType(CrossType.Pure).in(file("json-schema-circe"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-json-schema-circe",
      libraryDependencies += "io.circe" %%% "circe-core" % circeVersion
    )
    .jsConfigure(_.dependsOn(`json-schema-js` % "test->test;compile->compile"))
    .jvmConfigure(_.dependsOn(`json-schema-jvm` % "test->test;compile->compile"))
    .dependsOnLocalCrossProjects("algebra-circe") // Needed only because of CirceCodec, but that class doesn’t depend on the algebra

lazy val `json-schema-circe-js` = `json-schema-circe`.js
lazy val `json-schema-circe-jvm` = `json-schema-circe`.jvm