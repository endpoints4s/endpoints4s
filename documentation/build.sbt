import EndpointsSettings._
import LocalCrossProject._
import sbtcrossproject.CrossProject
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `algebra-playjson-jvm` = LocalProject("algebra-playjsonJVM")

val `play-client` = LocalProject("play-client")
val `play-server` = LocalProject("play-server")
val `play-server-circe` = LocalProject("play-server-circe")
val `play-server-playjson` = LocalProject("play-server-playjson")

val `akka-http-server` = LocalProject("akka-http-server")
val `akka-http-client` = LocalProject("akka-http-client")

val `xhr-client` = LocalProject("xhr-client")
val `xhr-client-circe` = LocalProject("xhr-client-circe")
val `xhr-client-faithful` = LocalProject("xhr-client-faithful")

val `scalaj-client` = LocalProject("scalaj-client")

val `sttp-client` = LocalProject("sttp-client")

val `openapi-jvm` = LocalProject("openapiJVM")

val `json-schema-jvm` = LocalProject("json-schemaJVM")
val `json-schema-circe-jvm` = LocalProject("json-schema-circeJVM")
val `json-schema-playjson-jvm` = LocalProject("json-schema-playjsonJVM")
val `json-schema-generic-jvm` = LocalProject("json-schema-genericJVM")

val apiDoc =
  project.in(file("api-doc"))
    .enablePlugins(ScalaUnidocPlugin)
    .settings(
      noPublishSettings,
      `scala 2.11`,
      coverageEnabled := false,
      scalacOptions in(ScalaUnidoc, unidoc) ++= Seq(
        "-diagrams",
        "-groups",
        "-doc-source-url", s"https://github.com/julienrf/endpoints/blob/v${version.value}€{FILE_PATH}.scala",
        "-sourcepath", (baseDirectory in ThisBuild).value.absolutePath
      ),
      unidocProjectFilter in(ScalaUnidoc, unidoc) := inProjects(
        `algebra-jvm`, `algebra-circe-jvm`, `algebra-playjson-jvm`,
        `akka-http-client`, `akka-http-server`,
        `play-client`, `play-server`, `play-server-circe`, `play-server-playjson`,
        `xhr-client`, `xhr-client-circe`, `xhr-client-faithful`,
        `scalaj-client`,
        `sttp-client`,
        `openapi-jvm`, `json-schema-jvm`, `json-schema-circe-jvm`, `json-schema-playjson-jvm`, `json-schema-generic-jvm`
      )
    )

val manual =
  project.in(file("manual"))
    .enablePlugins(OrnatePlugin, GhpagesPlugin)
    .settings(
      `scala 2.11`,
      coverageEnabled := false,
      git.remoteRepo := "git@github.com:julienrf/endpoints.git",
      ornateSettings := Map("version" -> version.value),
      siteSubdirName in ornate := "",
      addMappingsToSiteDir(mappings in ornate, siteSubdirName in ornate),
      mappings in ornate := {
        val output = ornate.value
        output ** AllPassFilter --- output pair relativeTo(output)
      },
      siteSubdirName in packageDoc := s"api/${version.value}",
      addMappingsToSiteDir(mappings in ScalaUnidoc in packageDoc in apiDoc, siteSubdirName in packageDoc),
      previewLaunchBrowser := false
    )


// Example for the “Overview” page of the documentation
val `example-quickstart-endpoints` =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure)
    .in(file("examples/quickstart/endpoints"))
    .settings(noPublishSettings, `scala 2.11 to latest`)
    .jsSettings(
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false
    )
    .jvmSettings(coverageEnabled := true)
    .dependsOnLocalCrossProjects("algebra", "json-schema-generic")

val `example-quickstart-endpoints-jvm` = `example-quickstart-endpoints`.jvm
val `example-quickstart-endpoints-js` = `example-quickstart-endpoints`.js

val `example-quickstart-client` =
  project.in(file("examples/quickstart/client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false,
      noPublishSettings,
      `scala 2.11 to latest`
    )
    .dependsOn(`example-quickstart-endpoints-js`, `xhr-client-circe`)

val `example-quickstart-server` =
  project.in(file("examples/quickstart/server"))
    .settings(
      noPublishSettings,
      `scala 2.11 to latest`,
      libraryDependencies += "org.scala-stm" %% "scala-stm" % "0.9"
    )
    .dependsOn(`example-quickstart-endpoints-jvm`, `play-server-playjson`, `openapi-jvm`)

// Basic example
val `example-basic-shared` = {
  val assetsDirectory = (base: File) => base / "src" / "main" / "assets"
  CrossProject("example-basic-shared", file("examples/basic/shared"))(JSPlatform, JVMPlatform).crossType(CrossType.Pure)
    .settings(
      noPublishSettings,
      `scala 2.11 to 2.12`,
      macroParadiseDependency,
      (sourceGenerators in Compile) += Def.task {
        assets.AssetsTasks.generateDigests(
          baseDirectory = baseDirectory.value.getParentFile,
          targetDirectory = (sourceManaged in Compile).value,
          generatedObjectName = "AssetsDigests",
          generatedPackage = Some("sample")
        )
      }.taskValue,
      libraryDependencies += "io.circe" %%% "circe-generic" % circeVersion
    )
    .jsSettings(
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false
    )
    .jvmSettings(
      coverageEnabled := false, // TODO Enable coverage when we add more tests
      (resourceGenerators in Compile) += Def.task {
        assets.AssetsTasks.gzipAssets(
          baseDirectory = baseDirectory.value.getParentFile,
          targetDirectory = (target in Compile).value
        )
      }.taskValue,
      unmanagedResourceDirectories in Compile += assetsDirectory(baseDirectory.value.getParentFile)
    )
    .enablePlugins(ScalaJSPlugin)
    .dependsOnLocalCrossProjects("algebra", "algebra-circe")
}

val `example-basic-shared-jvm` = `example-basic-shared`.jvm
val `example-basic-shared-js` = `example-basic-shared`.js

val `example-basic-client` =
  project.in(file("examples/basic/client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      noPublishSettings,
      `scala 2.11 to 2.12`,
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false
    )
    .dependsOn(`example-basic-shared-js`, `xhr-client-circe`)

val `example-basic-play-server` =
  project.in(file("examples/basic/play-server"))
    .settings(
      noPublishSettings,
      `scala 2.11 to 2.12`,
      unmanagedResources in Compile += (fastOptJS in(`example-basic-client`, Compile)).map(_.data).value,
      libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.25",
      libraryDependencies += "com.typesafe.play" %% "play" % playVersion
    )
    .dependsOn(`example-basic-shared-jvm`, `play-server`, `algebra-playjson-jvm`, `json-schema-playjson-jvm`, `openapi-jvm`)

val `example-basic-akkahttp-server` =
  project.in(file("examples/basic/akkahttp-server"))
    .settings(
      commonSettings,
      `scala 2.11 to 2.12`,
      publishArtifact := false
    )
    .dependsOn(`example-basic-shared-jvm`, `akka-http-server`)


// CQRS Example
// public endpoints definitions
val `example-cqrs-public-endpoints` =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure)
    .in(file("examples/cqrs/public-endpoints"))
    .settings(noPublishSettings, `scala 2.11 to latest`)
    .jsSettings(
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false
    )
    .jvmSettings(coverageEnabled := true)
    .settings(
      libraryDependencies += "io.circe" %%% "circe-generic" % circeVersion
    )
    .dependsOnLocalCrossProjects("example-cqrs-circe-instant", "json-schema-generic", "algebra-circe")

val `example-cqrs-public-endpoints-jvm` = `example-cqrs-public-endpoints`.jvm

val `example-cqrs-public-endpoints-js` = `example-cqrs-public-endpoints`.js

// web-client, *uses* the public endpoints’ definitions
val `example-cqrs-web-client` =
  project.in(file("examples/cqrs/web-client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      noPublishSettings,
      `scala 2.11 to 2.12`,
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false,
      libraryDependencies ++= Seq(
        "in.nvilla" %%% "monadic-html" % "0.2.3",
        "in.nvilla" %%% "monadic-rx-cats" % "0.2.3",
        "org.julienrf" %%% "faithful-cats" % "1.1.0",
        "org.scala-js" %%% "scalajs-java-time" % "0.2.5"
      ),
      scalaJSUseMainModuleInitializer := true
    )
    .dependsOn(`xhr-client-faithful`, `xhr-client-circe`)
    .dependsOn(`example-cqrs-public-endpoints-js`)

// public server implementation, *implements* the public endpoints’ definitions and *uses* the commands and queries definitions
val `example-cqrs-public-server` =
  project.in(file("examples/cqrs/public-server"))
    .settings(
      noPublishSettings,
      `scala 2.11 to 2.12`,
      unmanagedResources in Compile += (fastOptJS in (`example-cqrs-web-client`, Compile)).map(_.data).value,
      (sourceGenerators in Compile) += Def.task {
        assets.AssetsTasks.generateDigests(
          baseDirectory = (crossTarget in fastOptJS in `example-cqrs-web-client`).value,
          targetDirectory = (sourceManaged in Compile).value,
          generatedObjectName = "BootstrapDigests",
          generatedPackage = Some("cqrs.publicserver"),
          assetsPath = identity
        )
      }.dependsOn(fastOptJS in Compile in `example-cqrs-web-client`).taskValue
    )
    .dependsOn(`play-server-circe`, `play-client`, `openapi-jvm`)
    .dependsOn(`example-cqrs-public-endpoints-jvm`, `example-cqrs-commands-endpoints`, `example-cqrs-queries-endpoints`)

// commands endpoints definitions
lazy val `example-cqrs-commands-endpoints` =
  project.in(file("examples/cqrs/commands-endpoints"))
    .settings(
      noPublishSettings,
      `scala 2.11 to latest`,
      libraryDependencies ++= Seq(
        "org.scala-stm" %% "scala-stm" % "0.9",
        "io.circe" %% "circe-generic" % circeVersion
      )
    )
    .dependsOn(`algebra-circe-jvm`, `circe-instant-jvm`)

// commands implementation
val `example-cqrs-commands` =
  project.in(file("examples/cqrs/commands"))
    .settings(
      noPublishSettings,
      `scala 2.11 to latest`,
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
        scalaTestDependency
      )
    )
    .dependsOn(`play-server-circe`, `play-client` % Test)
    .dependsOn(`example-cqrs-commands-endpoints`)

// queries endpoints definitions
lazy val `example-cqrs-queries-endpoints` =
  project.in(file("examples/cqrs/queries-endpoints"))
    .settings(noPublishSettings, `scala 2.11 to latest`)
    .dependsOn(`algebra-circe-jvm`, `example-cqrs-public-endpoints-jvm` /* because we reuse the DTOs */)

// queries implementation
val `example-cqrs-queries` =
  project.in(file("examples/cqrs/queries"))
    .settings(noPublishSettings, `scala 2.11 to latest`)
    .dependsOn(`play-server-circe`, `play-client`)
    .dependsOn(`example-cqrs-queries-endpoints`, `example-cqrs-commands-endpoints`)

// this one exists only for the sake of simplifying the infrastructure: it runs all the HTTP services
val `example-cqrs` =
  project.in(file("examples/cqrs/infra"))
    .settings(noPublishSettings, `scala 2.11 to 2.12`)
    .settings(
      cancelable in Global := true,
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
        scalaTestDependency
      )
    )
    .dependsOn(`example-cqrs-queries`, `example-cqrs-commands`, `example-cqrs-public-server`, `example-cqrs-web-client`/*, `circe-instant-js`*/ /*, `circe-instant-jvm`*/)

lazy val `circe-instant` =
  CrossProject("example-cqrs-circe-instant", file("examples/cqrs/circe-instant"))(JSPlatform, JVMPlatform).crossType(CrossType.Pure)
    .settings(noPublishSettings, `scala 2.11 to latest`)
    .jsSettings(
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false
    )
    .jvmSettings(coverageEnabled := true)
    .settings(
      libraryDependencies += "io.circe" %%% "circe-core" % circeVersion
    )

lazy val `circe-instant-js` = `circe-instant`.js
lazy val `circe-instant-jvm` = `circe-instant`.jvm

val `example-documented` =
  project.in(file("examples/documented"))
    .settings(noPublishSettings, `scala 2.11 to latest`)
    .settings(
      herokuAppName in Compile := "documented-counter",
      herokuFatJar in Compile := Some((assemblyOutputPath in assembly).value),
      herokuSkipSubProjects in Compile := false,
      herokuProcessTypes in Compile := Map(
        "web" -> ("java -Dhttp.port=$PORT -jar " ++ (crossTarget.value / s"${name.value}-assembly-${version.value}.jar").relativeTo(baseDirectory.value).get.toString)
      ),
      assemblyMergeStrategy in assembly := {
        case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      (sourceGenerators in Compile) += Def.task {
        assets.AssetsTasks.generateDigests(
          baseDirectory = baseDirectory.value,
          targetDirectory = (sourceManaged in Compile).value,
          generatedObjectName = "AssetsDigests",
          generatedPackage = Some("counter"),
          assetsPath = _ / "src" / "main" / "resources" / "public"
        )
      }.taskValue
    )
    .dependsOn(`play-server-playjson`, `json-schema-generic-jvm`, `openapi-jvm`)

val `example-authentication` =
  project.in(file("examples/authentication"))
    .settings(noPublishSettings, `scala 2.11 to 2.12`)
    .settings(
      libraryDependencies ++= Seq(
        "com.pauldijou" %% "jwt-play" % "1.1.0",
        "com.lihaoyi"   %% "utest"    % "0.6.6"   % Test
      ),
      testFrameworks += new TestFramework("utest.runner.Framework")
    )
    .dependsOn(`play-server`, `play-client`, `algebra-playjson-jvm`)
