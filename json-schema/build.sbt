import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import EndpointsSettings._
import LocalCrossProject._

val `json-schema` =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("json-schema"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-algebra-json-schema",
      addScalaTestCrossDependency,
      libraryDependencies += "org.scala-lang.modules" %%% "scala-collection-compat" % "2.1.4",
      (Compile / boilerplateSource) := baseDirectory.value / ".." / "src" / "main" / "boilerplate"
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)

val `json-schema-js` = `json-schema`.js
val `json-schema-jvm` = `json-schema`.jvm

lazy val `json-schema-generic` =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("json-schema-generic"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-json-schema-generic",
      libraryDependencies += "com.chuusai" %%% "shapeless" % "2.3.3",
      addScalaTestCrossDependency,
      (Test / boilerplateSource) := baseDirectory.value / ".." / "src" / "test" / "boilerplate"
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .dependsOnLocalCrossProjects("json-schema")

lazy val `json-schema-generic-js` = `json-schema-generic`.js
lazy val `json-schema-generic-jvm` = `json-schema-generic`.jvm

lazy val `json-schema-circe` =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("json-schema-circe"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-json-schema-circe",
      libraryDependencies += "io.circe" %%% "circe-core" % circeVersion,
      (Compile / boilerplateSource) := baseDirectory.value / ".." / "src" / "main" / "boilerplate"
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .dependsOnLocalCrossProjects("algebra-circe") // Needed only because of CirceCodec, but that class doesn’t depend on the algebra
    .dependsOnLocalCrossProjectsWithScope("json-schema" -> "test->test;compile->compile")

lazy val `json-schema-circe-js` = `json-schema-circe`.js
lazy val `json-schema-circe-jvm` = `json-schema-circe`.jvm

lazy val `json-schema-playjson` =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("json-schema-playjson"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-json-schema-playjson",
      libraryDependencies += "com.typesafe.play" %%% "play-json" % playjsonVersion,
      (Compile / boilerplateSource) := baseDirectory.value / ".." / "src" / "main" / "boilerplate"
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .dependsOnLocalCrossProjectsWithScope("json-schema" -> "test->test;compile->compile")

lazy val `json-schema-playjson-js` = `json-schema-playjson`.js
lazy val `json-schema-playjson-jvm` = `json-schema-playjson`.jvm
