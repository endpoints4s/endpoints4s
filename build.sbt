import org.scalajs.sbtplugin.cross.CrossProject

val commonSettings = Seq(
  organization := "org.julienrf",
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
val `scala2.11` = Seq(
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.11.8")
)
val `scala2.12` = Seq(
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.11.8", "2.12.1")
)

val publishSettings = commonSettings ++ Seq(
  pomExtra :=
    <developers>
      <developer>
        <id>julienrf</id>
        <name>Julien Richard-Foy</name>
        <url>http://julien.richard-foy.fr</url>
      </developer>
    </developers>,
  scalacOptions in (Compile, doc) ++= Seq(
    "-doc-source-url", s"https://github.com/julienrf/endpoints/tree/v${version.value}€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath
  ),
  apiURL := Some(url(s"http://julienrf.github.io/${name.value}/${version.value}/api/")),
  autoAPIMappings := true,
  homepage := Some(url(s"https://github.com/julienrf/endpoints")),
  licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php")),
  scmInfo := Some(
    ScmInfo(
      url(s"https://github.com/julienrf/endpoints"),
      s"scm:git:git@github.com:julienrf/endpoints.git"
    )
  )
)

val noPublishSettings = Seq(
  publishArtifact := false,
  publish := (),
  publishLocal := ()
)

val algebra =
  crossProject.crossType(CrossType.Pure).in(file("algebra"))
    .settings(publishSettings: _*)
    .settings(`scala2.12`: _*)
    .settings(
      name := "endpoints-algebra"
    )

val `algebra-js` = algebra.js

val `algebra-jvm` = algebra.jvm

val circeVersion = "0.6.1"

val `algebra-circe` =
  crossProject.crossType(CrossType.Pure).in(file("algebra-circe"))
    .settings(publishSettings: _*)
    .settings(`scala2.12`: _*)
    .settings(
      name := "endpoints-algebra-circe",
      libraryDependencies += "io.circe" %%% "circe-generic" % circeVersion
    )
    .dependsOn(`algebra`)

val `algebra-circe-js` = `algebra-circe`.js

val `algebra-circe-jvm` = `algebra-circe`.jvm

val `xhr-client` =
  project.in(file("xhr-client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(publishSettings: _*)
    .settings(`scala2.12`: _*)
    .settings(
      name := "endpoints-xhr-client",
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"
    )
    .dependsOn(`algebra-js`)

val `xhr-client-faithful` =
  project.in(file("xhr-client-faithful"))
    .enablePlugins(ScalaJSPlugin)
    .settings(publishSettings: _*)
    .settings(`scala2.11`: _*)
    .settings(
      name := "endpoints-xhr-client-faithful",
      libraryDependencies += "org.julienrf" %%% "faithful" % "0.2"
    )
    .dependsOn(`xhr-client`)

val `xhr-client-circe` =
  project.in(file("xhr-client-circe"))
    .enablePlugins(ScalaJSPlugin)
    .settings(publishSettings: _*)
    .settings(`scala2.12`: _*)
    .settings(
      name := "endpoints-xhr-client-circe",
      libraryDependencies += "io.circe" %%% "circe-parser" % circeVersion
    )
    .dependsOn(`xhr-client`, `algebra-circe-js`)

val playVersion = "2.5.6"

val `play-circe` =
  project.in(file("play-circe"))
    .settings(publishSettings: _*)
    .settings(`scala2.11`: _*)
    .settings(
      name := "endpoints-play-circe",
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play" % playVersion,
        "io.circe" %% "circe-core" % circeVersion
      )
    )

val `play-server` =
  project.in(file("play-server"))
    .settings(publishSettings: _*)
    .settings(`scala2.11`: _*)
    .settings(
      name := "endpoints-play-server",
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-netty-server" % playVersion
      )
    )
    .dependsOn(`algebra-jvm`)

val `play-server-circe` =
  project.in(file("play-server-circe"))
    .settings(publishSettings: _*)
    .settings(`scala2.11`: _*)
    .settings(
      name := "endpoints-play-server-circe",
      libraryDependencies += "io.circe" %% "circe-jawn" % circeVersion
    )
    .dependsOn(`play-server`, `algebra-circe-jvm`, `play-circe`)

val `play-client` =
  project.in(file("play-client"))
      .settings(publishSettings: _*)
      .settings(`scala2.11`: _*)
      .settings(
        name := "endpoints-play-client",
        libraryDependencies += "com.typesafe.play" %% "play-ws" % playVersion
      )
      .dependsOn(`algebra-jvm`)

val `play-client-circe` =
  project.in(file("play-client-circe"))
    .settings(publishSettings: _*)
    .settings(`scala2.11`: _*)
    .settings(
      name := "endpoints-play-client-circe",
      libraryDependencies += "io.circe" %% "circe-jawn" % circeVersion
    )
    .dependsOn(`play-client`, `algebra-circe-jvm`, `play-circe`)

// Basic example
val `example-basic-shared` = {
  val assetsDirectory = (base: File) => base / "src" / "main" / "assets"
  CrossProject("example-basic-shared-jvm", "example-basic-shared-js", file("examples/basic/shared"), CrossType.Pure)
    .settings(commonSettings ++ noPublishSettings: _*)
    .settings(`scala2.12`: _*)
    .settings(
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
      (sourceGenerators in Compile) += Def.task {
        assets.AssetsTasks.generateDigests(
          baseDirectory = baseDirectory.value.getParentFile,
          targetDirectory = (target in Compile).value,
          generatedObjectName = "AssetsDigests",
          generatedPackage = Some("sample")
        )
      }.taskValue
    )
    .jvmSettings(
      (resourceGenerators in Compile) += Def.task {
        assets.AssetsTasks.gzipAssets(
          baseDirectory = baseDirectory.value.getParentFile,
          targetDirectory = (target in Compile).value
        )
      }.taskValue,
      unmanagedResourceDirectories in Compile += assetsDirectory(baseDirectory.value.getParentFile)
    )
    .enablePlugins(ScalaJSPlugin)
    .dependsOn(`algebra`, `algebra-circe`)
}

val `example-basic-shared-jvm` = `example-basic-shared`.jvm

val `example-basic-shared-js` = `example-basic-shared`.js

val `example-basic-client` =
  project.in(file("examples/basic/client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(commonSettings ++ noPublishSettings: _*)
    .settings(`scala2.12`: _*)
    .dependsOn(`example-basic-shared-js`, `xhr-client-circe`)

val `example-basic-server` =
  project.in(file("examples/basic/server"))
    .settings(commonSettings ++ noPublishSettings: _*)
    .settings(`scala2.11`: _*)
    .settings(
      unmanagedResources in Compile += (fastOptJS in (`example-basic-client`, Compile)).map(_.data).value,
      libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.2"
    )
    .dependsOn(`example-basic-shared-jvm`, `play-server-circe`)


// CQRS Example
// public endpoints definitions
val `example-cqrs-public-endpoints` =
  CrossProject("example-cqrs-public-endpoints-jvm", "example-cqrs-public-endpoints-js", file("examples/cqrs/public-endpoints"), CrossType.Pure)
    .settings(commonSettings ++ noPublishSettings ++ `scala2.12`: _*)
    .settings(
      libraryDependencies += "io.circe" %% "circe-java8" % circeVersion
    )
    .dependsOn(`algebra-circe`)

val `example-cqrs-public-endpoints-jvm` = `example-cqrs-public-endpoints`.jvm

val `example-cqrs-public-endpoints-js` = `example-cqrs-public-endpoints`.js

// public server implementation, *implements* the public endpoints’ definitions and *uses* the commands and queries definitions
val `example-cqrs-public-server` =
  project.in(file("examples/cqrs/public-server"))
    .settings(commonSettings ++ noPublishSettings ++ `scala2.11`: _*)
    .dependsOn(`play-server-circe`, `play-client-circe`)
    .dependsOn(`example-cqrs-public-endpoints-jvm`, `example-cqrs-commands-endpoints`, `example-cqrs-queries-endpoints`)

// web-client, *uses* the public endpoints’ definitions
val `example-cqrs-web-client` =
  project.in(file("examples/cqrs/web-client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(commonSettings ++ noPublishSettings ++ `scala2.11`: _*)
    .dependsOn(`xhr-client-faithful`, `xhr-client-circe`)
    .dependsOn(`example-cqrs-public-endpoints-js`)

// commands endpoints definitions
lazy val `example-cqrs-commands-endpoints` =
  project.in(file("examples/cqrs/commands-endpoints"))
    .settings(commonSettings ++ noPublishSettings ++ `scala2.12`: _*)
    .settings(
      libraryDependencies += "io.circe" %% "circe-java8" % circeVersion
    )
    .dependsOn(`algebra-circe-jvm`)

// commands implementation
val `example-cqrs-commands` =
  project.in(file("examples/cqrs/commands"))
    .settings(commonSettings ++ noPublishSettings ++ `scala2.11`: _*)
    .settings(
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.13.4" % Test,
        "org.scalatest" %% "scalatest" % "3.0.1" % Test
      )
    )
    .dependsOn(`play-server-circe`, `play-client-circe` % Test)
    .dependsOn(`example-cqrs-commands-endpoints`)

// queries endpoints definitions
lazy val `example-cqrs-queries-endpoints` =
  project.in(file("examples/cqrs/queries-endpoints"))
    .settings(commonSettings ++ noPublishSettings ++ `scala2.12`: _*)
    .dependsOn(`algebra-circe-jvm`, `example-cqrs-public-endpoints-jvm` /* because we reuse the DTOs */)

// queries implementation
val `example-cqrs-queries` =
  project.in(file("examples/cqrs/queries"))
    .settings(commonSettings ++ noPublishSettings ++ `scala2.11`: _*)
    .dependsOn(`play-server-circe`, `play-client-circe`)
    .dependsOn(`example-cqrs-queries-endpoints`, `example-cqrs-commands-endpoints`)

// this one exists only for the sake of simplifying the infrastructure: it runs all the HTTP services
val `example-cqrs-infra` =
  project.in(file("examples/cqrs/infra"))
    .settings(commonSettings ++ noPublishSettings ++ `scala2.11`: _*)
    .settings(
      cancelable in Global := true
    )
    .dependsOn(`example-cqrs-queries`, `example-cqrs-commands`, `example-cqrs-public-server`)

val endpoints =
  project.in(file("."))
    .enablePlugins(CrossPerProjectPlugin)
    .settings(noPublishSettings: _*)
      .settings(/*,
        releasePublishArtifactsAction := PgpKeys.publishSigned.value,
        releaseProcess := Seq[ReleaseStep](checkSnapshotDependencies,
          inquireVersions,
          runClean,
          runTest,
          setReleaseVersion,
          commitReleaseVersion,
          tagRelease,
          publishArtifacts,
          ReleaseStep(action = Command.process("publishDoc", _)),
          setNextVersion,
          commitNextVersion,
          ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
          pushChanges
        )*/
      )
    .aggregate(
      `algebra-js`, `algebra-jvm`,
      `algebra-circe-js`, `algebra-circe-jvm`,
      `play-circe`,
      `play-server`,
      `play-server-circe`,
      `xhr-client`,
      `xhr-client-faithful`,
      `xhr-client-circe`,
      `play-client`,
      `play-client-circe`,
      // basic example
      `example-basic-shared-js`, `example-basic-shared-jvm`,
      `example-basic-server`,
      `example-basic-client`,
      // cqrs example
      `example-cqrs-infra`,
      `example-cqrs-public-endpoints-jvm`, `example-cqrs-public-endpoints-js`,
      `example-cqrs-public-server`,
      `example-cqrs-web-client`,
      `example-cqrs-commands-endpoints`,
      `example-cqrs-commands`,
      `example-cqrs-queries-endpoints`,
      `example-cqrs-queries`
    )
