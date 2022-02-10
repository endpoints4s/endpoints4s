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
      name := "algebra"
    )
    .dependsOnLocalCrossProjects("json-schema")
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))

val `algebra-js` = algebra.js
val `algebra-jvm` = algebra.jvm

val `algebra-testkit` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("testkit"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "algebra-testkit",
      version := "2.0.0+n",
      libraryDependencies ++= Seq(
        ("com.typesafe.akka" %% "akka-http" % akkaHttpVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-actor" % akkaActorVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-stream" % akkaActorVersion).cross(CrossVersion.for3Use2_13),
        "com.lihaoyi" %% "ujson" % ujsonVersion
      )
    )
    .dependsOn(algebra)
    .dependsOnLocalCrossProjects("json-schema-testkit")
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))

val `algebra-testkit-js` = algebra.js
val `algebra-testkit-jvm` = algebra.jvm

val `algebra-circe` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("algebra-circe"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "algebra-circe",
      version := "2.1.0+n",
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
      version := "2.0.0+n",
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
