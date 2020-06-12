import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import EndpointsSettings._
import LocalCrossProject._

val algebra =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("algebra"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "algebra",
      libraryDependencies ++= Seq(
        "com.github.tomakehurst" % "wiremock" % "2.26.1" % Test,
        ("org.scalatest" %%% "scalatest" % scalaTestVersion % Test).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-http" % akkaHttpVersion % Test).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-actor" % akkaActorVersion % Test).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-stream" % akkaActorVersion % Test).withDottyCompat(scalaVersion.value),
        ("com.lihaoyi" %% "ujson" % ujsonVersion % Test).withDottyCompat(scalaVersion.value)
      )
    )
    .dependsOnLocalCrossProjectsWithScope(
      "json-schema" -> "test->test;compile->compile"
    )
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))

val `algebra-js` = algebra.js
val `algebra-jvm` = algebra.jvm

val `algebra-circe` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("algebra-circe"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "algebra-circe",
      libraryDependencies ++= Seq(
        ("io.circe" %%% "circe-parser" % circeVersion).withDottyCompat(scalaVersion.value),
        ("io.circe" %%% "circe-generic" % circeVersion % Test).withDottyCompat(scalaVersion.value)
      )
    )
    .dependsOn(`algebra` % "test->test;compile->compile")
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))

val `algebra-circe-js` = `algebra-circe`.js
val `algebra-circe-jvm` = `algebra-circe`.jvm

val `algebra-playjson` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("algebra-playjson"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "algebra-playjson",
      libraryDependencies += ("com.typesafe.play" %%% "play-json" % playjsonVersion).withDottyCompat(scalaVersion.value)
    )
    .dependsOn(`algebra` % "test->test;compile->compile")
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))

val `algebra-playjson-js` = `algebra-playjson`.js
val `algebra-playjson-jvm` = `algebra-playjson`.jvm
