import org.scalajs.sbtplugin.cross.CrossProject

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
      libraryDependencies += "org.typelevel" %%% "cats-core" % "0.4.1" // FIXME Lessen this dependency?
    )

val `algebra-js` = algebra.js

val `algebra-jvm` = algebra.jvm

val `algebra-circe` =
  crossProject.crossType(CrossType.Pure).in(file("algebra-circe"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "io.circe" %%% "circe-generic" % "0.4.0"
    )
    .dependsOn(`algebra`)

val `algebra-circe-js` = `algebra-circe`.js

val `algebra-circe-jvm` = `algebra-circe`.jvm

val `xhr-client` =
  project.in(file("xhr-client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.0"
    )
    .dependsOn(`algebra-js`)

val `xhr-client-circe` =
  project.in(file("xhr-client-circe"))
    .enablePlugins(ScalaJSPlugin)
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "io.circe" %%% "circe-parser" % "0.4.0"
    )
    .dependsOn(`xhr-client`, `algebra-circe-js`)

val `play-circe` =
  project.in(file("play-circe"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play" % "2.5.1",
        "io.circe" %% "circe-core" % "0.4.0"
      )
    )

val `play-server` =
  project.in(file("play-server"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-netty-server" % "2.5.1"
      )
    )
    .dependsOn(`algebra-jvm`)

val `play-server-circe` =
  project.in(file("play-server-circe"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "io.circe" %% "circe-jawn" % "0.4.0"
    )
    .dependsOn(`play-server`, `algebra-circe-jvm`, `play-circe`)

val `play-client` =
  project.in(file("play-client"))
      .settings(commonSettings: _*)
      .settings(
        libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.5.1"
      )
      .dependsOn(`algebra-jvm`)

val `play-client-circe` =
  project.in(file("play-client-circe"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "io.circe" %% "circe-jawn" % "0.4.0"
    )
    .dependsOn(`play-client`, `algebra-circe-jvm`, `play-circe`)

val `sample-shared` = {
  val assetsDirectory = (base: File) => base / "src" / "main" / "assets"
  CrossProject("sample-shared-jvm", "sample-shared-js", file("sample/shared"), CrossType.Pure)
    .settings(commonSettings: _*)
    .settings(
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
      (sourceGenerators in Compile) += Def.task {
        assets.AssetsTasks.generateDigests(
          baseDirectory = baseDirectory.value.getParentFile,
          targetDirectory = (target in Compile).value,
          generatedObjectName = "AssetsDigests",
          generatedPackage = Some("sample"),
          assetsPath = assetsDirectory
        )
      }.taskValue
    )
    .jvmSettings(
      unmanagedResourceDirectories in Compile += assetsDirectory(baseDirectory.value.getParentFile)
    )
    .enablePlugins(ScalaJSPlugin)
    .dependsOn(`algebra`, `algebra-circe`)
}

val `sample-shared-jvm` = `sample-shared`.jvm

val `sample-shared-js` = `sample-shared`.js

val `sample-client` =
  project.in(file("sample/client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(commonSettings: _*)
    .dependsOn(`sample-shared-js`, `xhr-client-circe`)

val `sample-server` =
  project.in(file("sample/server"))
    .settings(commonSettings: _*)
    .settings(
      unmanagedResources in Compile += (fastOptJS in (`sample-client`, Compile)).map(_.data).value
    )
    .dependsOn(`sample-shared-jvm`, `play-server-circe`)

val endpoints =
  project.in(file("."))
    .aggregate(
      `algebra-js`, `algebra-jvm`,
      `algebra-circe-js`, `algebra-circe-jvm`,
      `play-circe`,
      `play-server`,
      `play-server-circe`,
      `xhr-client`,
      `xhr-client-circe`,
      `play-client`,
      `play-client-circe`,
      `sample-shared-js`, `sample-shared-jvm`,
      `sample-server`,
      `sample-client`
    )
