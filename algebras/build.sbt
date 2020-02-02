import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import EndpointsSettings._
import LocalCrossProject._

val algebra =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("algebra"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-algebra",
      libraryDependencies ++= Seq(
        "com.github.tomakehurst" % "wiremock" % "2.25.1" % Test,
        "org.scalatest" %%% "scalatest" % scalaTestVersion % Test,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion % Test,
        "com.typesafe.akka" %% "akka-actor" % akkaActorVersion % Test,
        "com.typesafe.akka" %% "akka-stream" % akkaActorVersion % Test
      )
    )
    .dependsOnLocalCrossProjectsWithScope("json-schema" -> "test->test;compile->compile")

val `algebra-js` = algebra.js
val `algebra-jvm` = algebra.jvm

val `algebra-circe` =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("algebra-circe"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-algebra-circe",
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-parser" % circeVersion,
        "io.circe" %%% "circe-generic" % circeVersion % Test
      )
    )
    .dependsOn(`algebra` % "test->test;compile->compile")

val `algebra-circe-js` = `algebra-circe`.js
val `algebra-circe-jvm` = `algebra-circe`.jvm

val `algebra-playjson` =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("algebra-playjson"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-algebra-playjson",
      libraryDependencies += "com.typesafe.play" %%% "play-json" % playjsonVersion
    )
    .dependsOn(`algebra` % "test->test;compile->compile")

val `algebra-playjson-js` = `algebra-playjson`.js
val `algebra-playjson-jvm` = `algebra-playjson`.jvm
