val commonSettings = Seq(
  organization := "org.julienrf",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-encoding", "UTF-8",
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture",
    "-Xexperimental"
  )
)

val algebra =
  crossProject.crossType(CrossType.Pure).in(file("algebra"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "io.circe" %%% "circe-generic" % "0.2.1"
    )

val `algebra-js` = algebra.js

val `algebra-jvm` = algebra.jvm

val `client-xhr` =
  project.in(file("client-xhr"))
    .enablePlugins(ScalaJSPlugin)
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-parse" % "0.2.1",
        "org.scala-js" %%% "scalajs-dom" % "0.9.0"
      )
    )
    .dependsOn(`algebra-js`)

val `server-play` =
  project.in(file("server-play"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-netty-server" % "2.4.6",
        "io.circe" %% "circe-jawn" % "0.2.1"
      )
    )
    .dependsOn(`algebra-jvm`)

val `client-play` =
  project.in(file("client-play"))
      .settings(commonSettings: _*)
      .settings(
        libraryDependencies ++= Seq(
          "com.typesafe.play" %% "play-ws" % "2.4.3",
          "io.circe" %% "circe-jawn" % "0.2.1"
        )
      )
      .dependsOn(`algebra-jvm`)

val `endpoints` =
  project.in(file("."))
    .aggregate(`algebra-js`, `algebra-jvm`, `server-play`, `client-xhr`, `client-play`)

val `sample-shared` =
  crossProject.crossType(CrossType.Pure).in(file("sample/shared"))
    .settings(commonSettings: _*)
    .enablePlugins(ScalaJSPlugin)
    .dependsOn(`algebra`)

val `sample-shared-jvm` = `sample-shared`.jvm

val `sample-shared-js` = `sample-shared`.js

val `sample-client` =
  project.in(file("sample/client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(commonSettings: _*)
    .dependsOn(`sample-shared-js`, `client-xhr`)

val `sample-server` =
  project.in(file("sample/server"))
    .settings(commonSettings: _*)
    .settings(
      unmanagedResources in Compile += (fastOptJS in (`sample-client`, Compile)).map(_.data).value
    )
    .dependsOn(`sample-shared-jvm`, `server-play`)
