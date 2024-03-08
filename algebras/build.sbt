import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import EndpointsSettings._
import LocalCrossProject._

val algebra =
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("algebra"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "algebra"
    )
    .dependsOnLocalCrossProjectsWithNative("json-schema")
    .configurePlatforms(JSPlatform, NativePlatform)(_.disablePlugins(ScoverageSbtPlugin))

val `algebra-js` = algebra.js
val `algebra-jvm` = algebra.jvm
val `algebra-native` = algebra.native

val `algebra-testkit` =
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("testkit"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "algebra-testkit",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
        "org.apache.pekko" %% "pekko-actor" % pekkoActorVersion,
        "org.apache.pekko" %% "pekko-stream" % pekkoActorVersion,
        "com.lihaoyi" %% "ujson" % ujsonVersion
      )
    )
    .jsSettings(
      libraryDependencies += ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0").cross(
        CrossVersion.for3Use2_13
      )
    )
    .dependsOn(algebra)
    .dependsOnLocalCrossProjectsWithNative("json-schema-testkit")
    .configurePlatforms(JSPlatform, NativePlatform)(_.disablePlugins(ScoverageSbtPlugin))

val `algebra-testkit-js` = `algebra-testkit`.js
val `algebra-testkit-jvm` = `algebra-testkit`.jvm
val `algebra-testkit-native` = `algebra-testkit`.native

val `algebra-circe` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("algebra-circe"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "algebra-circe",
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-parser" % circeVersion,
        "io.circe" %%% "circe-generic" % circeVersion % Test
      )
    )
    .dependsOn(`algebra`, `algebra-testkit` % Test)
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))

val `algebra-circe-js` = `algebra-circe`.js
val `algebra-circe-jvm` = `algebra-circe`.jvm

val `algebra-circe-testkit` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("algebra-circe-testkit"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "algebra-circe-testkit",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq()
    )
    .dependsOn(`algebra-circe`, `algebra-testkit`)
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))

val `algebra-playjson` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("algebra-playjson"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "algebra-playjson",
      publish / skip := scalaBinaryVersion.value.startsWith("3"),
      libraryDependencies += ("com.typesafe.play" %%% "play-json" % playjsonVersion)
        .cross(CrossVersion.for3Use2_13)
    )
    .dependsOn(`algebra`, `algebra-testkit` % Test)
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))

val `algebra-playjson-js` = `algebra-playjson`.js
val `algebra-playjson-jvm` = `algebra-playjson`.jvm
