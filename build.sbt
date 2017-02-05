import org.scalajs.sbtplugin.cross.CrossProject
import sbtunidoc.Plugin.ScalaUnidoc
import sbtunidoc.Plugin.UnidocKeys.unidoc

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
  apiURL := Some(url(s"http://julienrf.github.io/endpoints/api/${version.value}/")),
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

val noPublishSettings = commonSettings ++ Seq(
  publishArtifact := false,
  publish := (),
  publishLocal := ()
)

val algebra =
  crossProject.crossType(CrossType.Pure).in(file("algebra"))
    .settings(publishSettings ++ `scala2.12`: _*)
    .settings(
      name := "endpoints-algebra"
    )

val `algebra-js` = algebra.js

val `algebra-jvm` = algebra.jvm

val circeVersion = "0.6.1"

val `algebra-circe` =
  crossProject.crossType(CrossType.Pure).in(file("algebra-circe"))
    .settings(publishSettings ++ `scala2.12`: _*)
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
    .settings(publishSettings ++ `scala2.12`: _*)
    .settings(
      name := "endpoints-xhr-client",
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"
    )
    .dependsOn(`algebra-js`)

val `xhr-client-faithful` =
  project.in(file("xhr-client-faithful"))
    .enablePlugins(ScalaJSPlugin)
    .settings(publishSettings ++ `scala2.11`: _*)
    .settings(
      name := "endpoints-xhr-client-faithful",
      libraryDependencies += "org.julienrf" %%% "faithful" % "0.2"
    )
    .dependsOn(`xhr-client`)

val `xhr-client-circe` =
  project.in(file("xhr-client-circe"))
    .enablePlugins(ScalaJSPlugin)
    .settings(publishSettings ++ `scala2.12`: _*)
    .settings(
      name := "endpoints-xhr-client-circe",
      libraryDependencies += "io.circe" %%% "circe-parser" % circeVersion
    )
    .dependsOn(`xhr-client`, `algebra-circe-js`)

val playVersion = "2.5.6"

val `play-circe` =
  project.in(file("play-circe"))
    .settings(publishSettings ++ `scala2.11`: _*)
    .settings(
      name := "endpoints-play-circe",
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play" % playVersion,
        "io.circe" %% "circe-core" % circeVersion
      )
    )

val `play-server` =
  project.in(file("play-server"))
    .settings(publishSettings ++ `scala2.11`: _*)
    .settings(
      name := "endpoints-play-server",
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-netty-server" % playVersion
      )
    )
    .dependsOn(`algebra-jvm`)

val `play-server-circe` =
  project.in(file("play-server-circe"))
    .settings(publishSettings ++ `scala2.11`: _*)
    .settings(
      name := "endpoints-play-server-circe",
      libraryDependencies += "io.circe" %% "circe-jawn" % circeVersion
    )
    .dependsOn(`play-server`, `algebra-circe-jvm`, `play-circe`)

val `play-client` =
  project.in(file("play-client"))
      .settings(publishSettings ++ `scala2.11`: _*)
      .settings(
        name := "endpoints-play-client",
        libraryDependencies += "com.typesafe.play" %% "play-ws" % playVersion
      )
      .dependsOn(`algebra-jvm`)

val `play-client-circe` =
  project.in(file("play-client-circe"))
    .settings(publishSettings ++ `scala2.11`: _*)
    .settings(
      name := "endpoints-play-client-circe",
      libraryDependencies += "io.circe" %% "circe-jawn" % circeVersion
    )
    .dependsOn(`play-client`, `algebra-circe-jvm`, `play-circe`)

val `akka-http-server` =
  project.in(file("akka-http-server"))
    .settings(publishSettings: _*)
    .settings(`scala2.12`: _*)
    .settings(
      name := "endpoints-akka-http-server",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http" % "10.0.1"
      )
    )
    .dependsOn(`algebra-jvm`)

val `akka-http-server-circe` =
  project.in(file("akka-http-server-circe"))
    .settings(publishSettings: _*)
    .settings(`scala2.12`: _*)
    .settings(
      name := "endpoints-akka-http-server-circe",
      libraryDependencies ++= Seq(
        "de.heikoseeberger" %% "akka-http-circe" % "1.11.0"
      )
    )
    .dependsOn(`akka-http-server`, `algebra-circe-jvm`)

val apiDoc =
  project.in(file("api-doc"))
    .settings(noPublishSettings ++ `scala2.11` ++ unidocSettings: _*)
    .settings(
      scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
        "-groups",
        "-doc-source-url", s"https://github.com/julienrf/endpoints/blob/v${version.value}€{FILE_PATH}.scala",
        "-sourcepath", (baseDirectory in ThisBuild).value.absolutePath
      ),
      sbtunidoc.Plugin.UnidocKeys.unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(
        `algebra-jvm`, `algebra-circe-jvm`,
        `play-circe`,
        `play-client`, `play-client-circe`,
        `play-server`, `play-server-circe`,
        `xhr-client`, `xhr-client-circe`, `xhr-client-faithful`
      )
    )

val ornateTarget = Def.setting(target.value / "ornate")

val manual =
  project.in(file("manual"))
    .enablePlugins(OrnatePlugin)
    .settings(noPublishSettings ++ ghpages.settings: _*)
    .settings(
      scalaVersion := "2.11.8",
      git.remoteRepo := "git@github.com:julienrf/endpoints.git",
      ornateSourceDir := Some(sourceDirectory.value / "ornate"),
      ornateTargetDir := Some(ornateTarget.value),
      ornateSettings := Map("version" -> version.value),
      siteSubdirName in ornate := "",
      addMappingsToSiteDir(mappings in ornate, siteSubdirName in ornate),
      mappings in ornate := {
        val _ = ornate.value
        val output = ornateTarget.value
        output ** AllPassFilter --- output pair relativeTo(output)
      },
      siteSubdirName in packageDoc := s"api/${version.value}",
      addMappingsToSiteDir(mappings in ScalaUnidoc in packageDoc in apiDoc, siteSubdirName in packageDoc),
      previewLaunchBrowser := false
    )


// Example for the “Overview” page of the documentation
val `example-overview-endpoints` =
  crossProject.crossType(CrossType.Pure)
    .in(file("examples/overview/endpoints"))
    .settings(noPublishSettings ++ `scala2.12`: _*)
    .settings(
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
    )
    .dependsOn(`algebra-circe`)

val `example-overview-endpoints-jvm` = `example-overview-endpoints`.jvm

val `example-overview-endpoints-js` = `example-overview-endpoints`.js

val `example-overview-client` =
  project.in(file("examples/overview/client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(noPublishSettings ++ `scala2.12`: _*)
    .dependsOn(`example-overview-endpoints-js`, `xhr-client-circe`)

val `example-overview-server` =
  project.in(file("examples/overview/server"))
    .settings(noPublishSettings ++ `scala2.11`: _*)
    .dependsOn(`example-overview-endpoints-jvm`, `play-server-circe`)

val `example-overview-play-client` =
  project.in(file("examples/overview/play-client"))
    .settings(noPublishSettings ++ `scala2.11`: _*)
    .dependsOn(`example-overview-endpoints-jvm`, `play-client-circe`)

// Basic example
val `example-basic-shared` = {
  val assetsDirectory = (base: File) => base / "src" / "main" / "assets"
  CrossProject("example-basic-shared-jvm", "example-basic-shared-js", file("examples/basic/shared"), CrossType.Pure)
    .settings(noPublishSettings ++ `scala2.12`: _*)
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
    .settings(noPublishSettings ++ `scala2.12`: _*)
    .dependsOn(`example-basic-shared-js`, `xhr-client-circe`)

val `example-basic-play-server` =
  project.in(file("examples/basic/play-server"))
    .settings(noPublishSettings ++ `scala2.11`: _*)
    .settings(
      unmanagedResources in Compile += (fastOptJS in (`example-basic-client`, Compile)).map(_.data).value,
      libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.2"
    )
    .dependsOn(`example-basic-shared-jvm`, `play-server-circe`)

val `example-basic-akkahttp-server` =
  project.in(file("examples/basic/akkahttp-server"))
    .settings(commonSettings: _*)
    .settings(`scala2.12`: _*)
    .settings(
      publishArtifact := false
    )
    .dependsOn(`example-basic-shared-jvm`, `akka-http-server-circe`)


// CQRS Example
// public endpoints definitions
val `example-cqrs-public-endpoints` =
  CrossProject("example-cqrs-public-endpoints-jvm", "example-cqrs-public-endpoints-js", file("examples/cqrs/public-endpoints"), CrossType.Pure)
    .settings(noPublishSettings ++ `scala2.12`: _*)
    .dependsOn(`algebra-circe`, `circe-instant`)

val `example-cqrs-public-endpoints-jvm` = `example-cqrs-public-endpoints`.jvm

val `example-cqrs-public-endpoints-js` = `example-cqrs-public-endpoints`.js

// web-client, *uses* the public endpoints’ definitions
val `example-cqrs-web-client` =
  project.in(file("examples/cqrs/web-client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(noPublishSettings ++ `scala2.11`: _*)
    .settings(
      libraryDependencies ++= Seq(
        "in.nvilla" %%% "monadic-html" % "0.2.2",
        "in.nvilla" %%% "monadic-rx-cats" % "0.2.2",
        "org.julienrf" %%% "faithful-cats" % "0.2",
        "org.scala-js" %%% "scalajs-java-time" % "0.2.0"
      ),
      persistLauncher := true
    )
    .dependsOn(`xhr-client-faithful`, `xhr-client-circe`)
    .dependsOn(`example-cqrs-public-endpoints-js`)

// public server implementation, *implements* the public endpoints’ definitions and *uses* the commands and queries definitions
val `example-cqrs-public-server` =
  project.in(file("examples/cqrs/public-server"))
    .settings(noPublishSettings ++ `scala2.11`: _*)
    .settings(
      libraryDependencies += "com.typesafe.play" %% "twirl-api" % "1.2.0",
      unmanagedResources in Compile ++= Seq(
        (fastOptJS in (`example-cqrs-web-client`, Compile)).map(_.data).value,
        (packageScalaJSLauncher in (`example-cqrs-web-client`, Compile)).map(_.data).value
      ),
      (sourceGenerators in Compile) += Def.task {
        assets.AssetsTasks.generateDigests(
          baseDirectory = (crossTarget in fastOptJS in `example-cqrs-web-client`).value,
          targetDirectory = (target in Compile).value,
          generatedObjectName = "BootstrapDigests",
          generatedPackage = Some("cqrs.publicserver"),
          assetsPath = identity
        )
      }.dependsOn(fastOptJS in Compile in `example-cqrs-web-client`).taskValue
    )
    .dependsOn(`play-server-circe`, `play-client-circe`)
    .dependsOn(`example-cqrs-public-endpoints-jvm`, `example-cqrs-commands-endpoints`, `example-cqrs-queries-endpoints`)

// commands endpoints definitions
lazy val `example-cqrs-commands-endpoints` =
  project.in(file("examples/cqrs/commands-endpoints"))
    .settings(noPublishSettings ++ `scala2.12`: _*)
    .dependsOn(`algebra-circe-jvm`, `circe-instant-jvm`)

// commands implementation
val `example-cqrs-commands` =
  project.in(file("examples/cqrs/commands"))
    .settings(noPublishSettings ++ `scala2.11`: _*)
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
    .settings(noPublishSettings ++ `scala2.12`: _*)
    .dependsOn(`algebra-circe-jvm`, `example-cqrs-public-endpoints-jvm` /* because we reuse the DTOs */)

// queries implementation
val `example-cqrs-queries` =
  project.in(file("examples/cqrs/queries"))
    .settings(noPublishSettings ++ `scala2.11`: _*)
    .dependsOn(`play-server-circe`, `play-client-circe`)
    .dependsOn(`example-cqrs-queries-endpoints`, `example-cqrs-commands-endpoints`)

// this one exists only for the sake of simplifying the infrastructure: it runs all the HTTP services
val `example-cqrs` =
  project.in(file("examples/cqrs/infra"))
    .settings(noPublishSettings ++ `scala2.11`: _*)
    .settings(
      cancelable in Global := true,
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.13.4" % Test,
        "org.scalatest" %% "scalatest" % "3.0.1" % Test
      )
    )
    .dependsOn(`example-cqrs-queries`, `example-cqrs-commands`, `example-cqrs-public-server`, `example-cqrs-web-client`, `circe-instant-js`, `circe-instant-jvm`)

lazy val `circe-instant` =
  CrossProject("example-cqrs-circe-instant-jvm", "example-cqrs-circe-instante-js", file("examples/cqrs/circe-instant"), CrossType.Pure)
    .settings(noPublishSettings ++ `scala2.11`: _*)
    .settings(
      libraryDependencies += "io.circe" %%% "circe-core" % circeVersion
    )

lazy val `circe-instant-js` = `circe-instant`.js
lazy val `circe-instant-jvm` = `circe-instant`.jvm

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
      `akka-http-server`,
      `akka-http-server-circe`,
      // overview example
      `example-overview-endpoints-js`, `example-overview-endpoints-jvm`,
      `example-overview-server`,
      `example-overview-client`,
      `example-overview-play-client`,
      // basic example
      `example-basic-shared-js`, `example-basic-shared-jvm`,
      `example-basic-play-server`,
      `example-basic-akkahttp-server`,
      `example-basic-client`,
      // cqrs example
      `example-cqrs`,
      `example-cqrs-public-endpoints-jvm`, `example-cqrs-public-endpoints-js`,
      `example-cqrs-public-server`,
      `example-cqrs-web-client`,
      `example-cqrs-commands-endpoints`,
      `example-cqrs-commands`,
      `example-cqrs-queries-endpoints`,
      `example-cqrs-queries`
    )
