import EndpointsSettings._
import LocalCrossProject._

val testsuite = LocalProject("testsuite")

val `json-schema` =
  crossProject.crossType(CrossType.Pure).in(file("json-schema"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-openapi-json-schema",
      addScalaTestCrossDependency
    )

val `json-schema-js` = `json-schema`.js
val `json-schema-jvm` = `json-schema`.jvm

val `json-schema-generic` =
  crossProject.crossType(CrossType.Pure).in(file("json-schema-generic"))
    .settings(publishSettings ++ `scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-openapi-json-schema-generic",
      libraryDependencies += "com.chuusai" %%% "shapeless" % "2.3.2",
      addScalaTestCrossDependency
    )
    .dependsOn(`json-schema`)

val `json-schema-generic-js` = `json-schema-generic`.js
val `json-schema-generic-jvm` = `json-schema-generic`.jvm

val `json-schema-circe` =
  crossProject.crossType(CrossType.Pure).in(file("json-schema-circe"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-openapi-json-schema-circe",
      libraryDependencies += "io.circe" %%% "circe-core" % circeVersion
    )
    .dependsOn(`json-schema` % "test->test;compile->compile")
    .dependsOnLocalCrossProjects("algebra-circe") // Needed only because of CirceCodec, but that class doesn’t depend on the algebra

val `json-schema-circe-js` = `json-schema-circe`.js
val `json-schema-circe-jvm` = `json-schema-circe`.jvm

val openapi =
  crossProject.crossType(CrossType.Pure).in(file("openapi"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-openapi",
      libraryDependencies += "io.circe" %%% "circe-core" % circeVersion
    )
    .dependsOn(`json-schema` % "test->test;compile->compile")
    .jsConfigure(_.dependsOn(LocalProject("testsuiteJS") % Test))
    .jvmConfigure(_.dependsOn(LocalProject("testsuiteJVM") % Test))
    .dependsOnLocalCrossProjects("algebra")

val `openapi-js` = openapi.js
val `openapi-jvm` = openapi.jvm

val `openapi-circe` =
  crossProject.crossType(CrossType.Pure).in(file("circe"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-openapi-circe",
      libraryDependencies += "io.circe" %%% "circe-core" % circeVersion
    )
    .dependsOn(openapi, `json-schema-circe`)
    .jsConfigure(_.dependsOn(LocalProject("testsuiteJS") % Test))
    .jvmConfigure(_.dependsOn(LocalProject("testsuiteJVM") % Test))
    .dependsOnLocalCrossProjects("algebra-circe")

val `openapi-circe-js` = `openapi-circe`.js
val `openapi-circe-jvm` = `openapi-circe`.jvm
