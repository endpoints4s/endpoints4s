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
    .nativeSettings(
      versionPolicyFirstVersion := Some("1.8.0")
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
      version := "3.0.0",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        ("com.typesafe.akka" %% "akka-http" % akkaHttpVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-actor" % akkaActorVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-stream" % akkaActorVersion).cross(CrossVersion.for3Use2_13),
        "com.lihaoyi" %% "ujson" % ujsonVersion
      )
    )
    .nativeSettings(
      versionPolicyFirstVersion := Some("2.1.0")
    )
    .dependsOn(algebra)
    .dependsOnLocalCrossProjectsWithNative("json-schema-testkit")
    .configurePlatforms(JSPlatform, NativePlatform)(_.disablePlugins(ScoverageSbtPlugin))

val `algebra-testkit-js` = algebra.js
val `algebra-testkit-jvm` = algebra.jvm
val `algebra-testkit-native` = algebra.native

val `algebra-circe` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("algebra-circe"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "algebra-circe",
      version := "2.2.0",
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
      version := "3.0.0",
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
      libraryDependencies += ("com.typesafe.play" %%% "play-json" % playjsonVersion).cross(CrossVersion.for3Use2_13)
    )
    .dependsOn(`algebra`, `algebra-testkit` % Test)
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))

val `algebra-playjson-js` = `algebra-playjson`.js
val `algebra-playjson-jvm` = `algebra-playjson`.jvm
