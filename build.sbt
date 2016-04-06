val commonSettings = Seq(
  organization := "org.julienrf",
  scalaVersion := "2.11.7",
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

val `endpoints-algebra` =
  crossProject.crossType(CrossType.Pure).in(file("endpoints-algebra"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "io.circe" %%% "circe-generic" % "0.2.1"
    )

val `endpoints-algebra-js` = `endpoints-algebra`.js

val `endpoints-algebra-jvm` = `endpoints-algebra`.jvm

val `endpoints-client-xhr` =
  project.in(file("endpoints-client-xhr"))
    .enablePlugins(ScalaJSPlugin)
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-parse" % "0.2.1",
        "org.scala-js" %%% "scalajs-dom" % "0.8.2"
      )
    )
    .dependsOn(`endpoints-algebra-js`)

val `endpoints-server-play` =
  project.in(file("endpoints-server-play"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-netty-server" % "2.4.6",
        "io.circe" %% "circe-jawn" % "0.2.1"
      )
    )
    .dependsOn(`endpoints-algebra-jvm`)

val `endpoints-client-play` =
  project.in(file("endpoints-client-play"))
      .settings(commonSettings: _*)
      .settings(
        libraryDependencies ++= Seq(
          "com.typesafe.play" %% "play-ws" % "2.4.3",
          "io.circe" %% "circe-jawn" % "0.2.1"
        )
      )
      .dependsOn(`endpoints-algebra-jvm`)

val `endpoints` =
  project.in(file("."))
    .aggregate(`endpoints-algebra-js`, `endpoints-algebra-jvm`, `endpoints-server-play`, `endpoints-client-xhr`, `endpoints-client-play`)

val `sample-shared` =
  crossProject.crossType(CrossType.Pure).in(file("sample/shared"))
    .settings(commonSettings: _*)
    .enablePlugins(ScalaJSPlugin)
    .dependsOn(`endpoints-algebra`)

val `sample-shared-jvm` = `sample-shared`.jvm

val `sample-shared-js` = `sample-shared`.js

val `sample-client` =
  project.in(file("sample/client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(commonSettings: _*)
    .dependsOn(`sample-shared-js`, `endpoints-client-xhr`)

val `sample-server` =
  project.in(file("sample/server"))
    .settings(commonSettings: _*)
    .settings(
      unmanagedResources in Compile += (fastOptJS in (`sample-client`, Compile)).map(_.data).value
    )
    .dependsOn(`sample-shared-jvm`, `endpoints-server-play`)
