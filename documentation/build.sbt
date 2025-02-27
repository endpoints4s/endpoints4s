import EndpointsSettings._
import LocalCrossProject._
import sbtcrossproject.CrossProject
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}
import xerial.sbt.Sonatype.GitHubHosting
import com.lightbend.paradox.markdown.Writer

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `algebra-playjson-jvm` = LocalProject("algebra-playjsonJVM")

val `pekko-http-client` = LocalProject("pekko-http-client")
val `pekko-http-server` = LocalProject("pekko-http-server")

val `http4s-client-jvm` = LocalProject("http4s-clientJVM")
val `http4s-server` = LocalProject("http4s-server")

val `xhr-client` = LocalProject("xhr-client")
val `xhr-client-circe` = LocalProject("xhr-client-circe")
val `xhr-client-faithful` = LocalProject("xhr-client-faithful")

val `sttp-client` = LocalProject("sttp-client")

val `openapi-jvm` = LocalProject("openapiJVM")

val `json-schema-jvm` = LocalProject("json-schemaJVM")
val `json-schema-circe-jvm` = LocalProject("json-schema-circeJVM")
val `json-schema-playjson-jvm` = LocalProject("json-schema-playjsonJVM")
val `json-schema-generic-jvm` = LocalProject("json-schema-genericJVM")

val `fetch-client` = LocalProject("fetch-client")
val `fetch-client-circe` = LocalProject("fetch-client-circe")

val apiDoc =
  project
    .in(file("api-doc"))
    .enablePlugins(ScalaUnidocPlugin)
    .settings(
      noPublishSettings,
      version := (`algebra-jvm` / version).value,
      `scala 2.13`,
      coverageEnabled := false,
      ScalaUnidoc / unidoc / scalacOptions ++= Seq(
        "-implicits",
        "-diagrams",
        "-groups",
        "-doc-source-url",
        s"https://github.com/endpoints4s/endpoints4s/blob/v${version.value}€{FILE_PATH}.scala",
        "-sourcepath",
        (ThisBuild / baseDirectory).value.absolutePath
      ),
      ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
        `algebra-jvm`,
        `algebra-circe-jvm`,
        `algebra-playjson-jvm`,
        `pekko-http-client`,
        `pekko-http-server`,
        `http4s-client-jvm`,
        `http4s-server`,
        `xhr-client`,
        `xhr-client-circe`,
        `xhr-client-faithful`,
        `sttp-client`,
        `openapi-jvm`,
        `json-schema-jvm`,
        `json-schema-circe-jvm`,
        `json-schema-playjson-jvm`,
        `json-schema-generic-jvm`,
        `fetch-client`,
        `fetch-client-circe`
      )
    )

val manual =
  project
    .in(file("manual"))
    .enablePlugins(
      ParadoxMaterialThemePlugin,
      ParadoxPlugin,
      ParadoxSitePlugin,
      SitePreviewPlugin,
      GhpagesPlugin
    )
    .settings(
      noPublishSettings,
      `scala 2.13`,
      version := (`algebra-jvm` / version).value,
      coverageEnabled := false,
      git.remoteRepo := "git@github.com:endpoints4s/endpoints4s.github.io.git",
      ghpagesBranch := "master",
      Compile / paradoxMaterialTheme := {
        val theme = (Compile / paradoxMaterialTheme).value
        val repository =
          (ThisBuild / sonatypeProjectHosting).value.get.scmInfo.browseUrl.toURI
        theme
          .withRepository(repository)
          .withSocial(repository)
          .withCustomStylesheet("snippets.css")
      },
      paradoxProperties ++= Map(
        "version" -> version.value,
        "pekko-http-server-version" -> (`pekko-http-server` / version).value,
        "fetch-client-version" -> (`fetch-client` / version).value,
        "pekko-version" -> pekkoActorVersion,
        "pekko-http-version" -> pekkoHttpVersion,
        "scaladoc.base_url" -> s".../${(packageDoc / siteSubdirName).value}",
        "github.base_url" -> s"${(ThisBuild / sonatypeProjectHosting).value.get.scmInfo.browseUrl}/blob/v${version.value}"
      ),
      paradoxDirectives += ((_: Writer.Context) =>
        org.endpoints4s.paradox.coordinates.CoordinatesDirective
      ),
      packageDoc / siteSubdirName := "api",
      addMappingsToSiteDir(
        apiDoc / ScalaUnidoc / packageDoc / mappings,
        packageDoc / siteSubdirName
      ),
      makeSite / mappings ++= {
        val gvSourceDirectory = (Compile / sourceDirectory).value / "graphviz"
        val gvTargetDirectory = target.value / "graphviz"
        (gvSourceDirectory ** "*.gv")
          .get()
          .pair(Path.relativeTo(gvSourceDirectory))
          .map { case (sourceFile, relativePath) =>
            import scala.sys.process._
            val targetRelativePath = s"${relativePath.stripSuffix(".gv")}.svg"
            val targetFile = gvTargetDirectory / targetRelativePath
            IO.createDirectory(targetFile.getParentFile)
            assert(s"dot -Tsvg -o${targetFile} ${sourceFile}".! == 0)
            (targetFile, targetRelativePath)
          }
      },
      previewLaunchBrowser := false
    )

// Example for the “Overview” page of the documentation
val `example-quickstart-endpoints` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("examples/quickstart/endpoints"))
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))
    .settings(noPublishSettings, `scala 2.12 to dotty`)
    .jvmSettings(coverageEnabled := true)
    .dependsOnLocalCrossProjects("algebra", "json-schema-generic")

val `example-quickstart-endpoints-jvm` = `example-quickstart-endpoints`.jvm
val `example-quickstart-endpoints-js` = `example-quickstart-endpoints`.js

val `example-quickstart-client` =
  project
    .in(file("examples/quickstart/client"))
    .enablePlugins(ScalaJSPlugin)
    .configure(_.disablePlugins(ScoverageSbtPlugin))
    .settings(
      noPublishSettings,
      `scala 2.12 to dotty`
    )
    .dependsOn(`example-quickstart-endpoints-js`, `fetch-client`)

val `example-quickstart-server` =
  project
    .in(file("examples/quickstart/server"))
    .settings(
      noPublishSettings,
      `scala 2.12 to 2.13`,
      libraryDependencies ++= Seq(
        "org.scala-stm" %% "scala-stm" % "0.11.1",
        "org.apache.pekko" %% "pekko-stream" % pekkoActorVersion,
        scalaTestDependency
      )
    )
    .dependsOn(
      `example-quickstart-endpoints-jvm`,
      `pekko-http-server`,
      `openapi-jvm`
    )

// Basic example
val `example-basic-shared` = {
  val assetsDirectory = (base: File) => base / "src" / "main" / "assets"
  CrossProject("example-basic-shared", file("examples/basic/shared"))(
    JSPlatform,
    JVMPlatform
  ).crossType(CrossType.Pure)
    .settings(
      noPublishSettings,
      `scala 2.12 to 2.13`,
      macroParadiseDependency,
      Compile / sourceGenerators += Def.task {
        assets.AssetsTasks.generateDigests(
          baseDirectory = baseDirectory.value.getParentFile,
          targetDirectory = (Compile / sourceManaged).value,
          generatedObjectName = "AssetsDigests",
          generatedPackage = Some("sample")
        )
      }.taskValue,
      libraryDependencies += "io.circe" %%% "circe-generic" % circeVersion
    )
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))
    .jvmSettings(
      coverageEnabled := false, // TODO Enable coverage when we add more tests
      Compile / resourceGenerators += Def.task {
        assets.AssetsTasks.gzipAssets(
          baseDirectory = baseDirectory.value.getParentFile,
          targetDirectory = (Compile / target).value
        )
      }.taskValue,
      Compile / unmanagedResourceDirectories += assetsDirectory(
        baseDirectory.value.getParentFile
      )
    )
    .enablePlugins(ScalaJSPlugin)
    .dependsOnLocalCrossProjects("algebra", "algebra-circe")
}

val `example-basic-shared-jvm` = `example-basic-shared`.jvm
val `example-basic-shared-js` = `example-basic-shared`.js

val `example-basic-client` =
  project
    .in(file("examples/basic/client"))
    .enablePlugins(ScalaJSPlugin)
    .configure(_.disablePlugins(ScoverageSbtPlugin))
    .settings(
      noPublishSettings,
      `scala 2.12 to 2.13`,
      scalaJSUseMainModuleInitializer := true,
      libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.3.0"
    )
    .dependsOn(`example-basic-shared-js`, `fetch-client-circe`)

val `example-basic-pekkohttp-server` =
  project
    .in(file("examples/basic/pekkohttp-server"))
    .settings(
      noPublishSettings,
      `scala 2.12 to 2.13`,
      libraryDependencies += "org.apache.pekko" %% "pekko-stream" % pekkoActorVersion,
      publishArtifact := false
    )
    .dependsOn(`example-basic-shared-jvm`, `pekko-http-server`)

// CQRS Example
// public endpoints definitions
val `example-cqrs-public-endpoints` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("examples/cqrs/public-endpoints"))
    .settings(noPublishSettings, `scala 2.12 to dotty`)
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))
    .jvmSettings(coverageEnabled := true)
    .settings(
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-generic" % circeVersion,
        "io.github.cquiroz" %%% "scala-java-time" % "2.3.0"
      )
    )
    .dependsOnLocalCrossProjects("json-schema-generic", "algebra-circe")

val `example-cqrs-public-endpoints-jvm` = `example-cqrs-public-endpoints`.jvm
val `example-cqrs-public-endpoints-js` = `example-cqrs-public-endpoints`.js

// web-client, *uses* the public endpoints’ definitions
val `example-cqrs-web-client` =
  project
    .in(file("examples/cqrs/web-client"))
    .enablePlugins(ScalaJSPlugin)
    .configure(_.disablePlugins(ScoverageSbtPlugin))
    .settings(
      noPublishSettings,
      `scala 2.12 to 2.13`,
      libraryDependencies ++= Seq(
        "com.raquo" %%% "laminar" % "0.14.5",
        "io.github.cquiroz" %%% "scala-java-time" % "2.2.2",
        ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0").cross(CrossVersion.for3Use2_13)
      ),
      scalaJSUseMainModuleInitializer := true
    )
    .dependsOn(`fetch-client-circe`)
    .dependsOn(`example-cqrs-public-endpoints-js`)

// public server implementation, *implements* the public endpoints’ definitions and *uses* the commands and queries definitions
val `example-cqrs-public-server` =
  project
    .in(file("examples/cqrs/public-server"))
    .settings(
      noPublishSettings,
      `scala 2.12 to 2.13`,
      Compile / unmanagedResources += (`example-cqrs-web-client` / Compile / fastOptJS)
        .map(_.data)
        .value,
      Compile / sourceGenerators += Def
        .task {
          assets.AssetsTasks.generateDigests(
            baseDirectory = (`example-cqrs-web-client` / fastOptJS / crossTarget).value,
            targetDirectory = (Compile / sourceManaged).value,
            generatedObjectName = "BootstrapDigests",
            generatedPackage = Some("cqrs.publicserver"),
            assetsPath = identity
          )
        }
        .dependsOn(`example-cqrs-web-client` / Compile / fastOptJS)
        .taskValue
    )
    .dependsOn(`http4s-server`, `http4s-client-jvm`, `openapi-jvm`)
    .dependsOn(
      `example-cqrs-public-endpoints-jvm`,
      `example-cqrs-commands-endpoints`,
      `example-cqrs-queries-endpoints`
    )

// commands endpoints definitions
lazy val `example-cqrs-commands-endpoints` =
  project
    .in(file("examples/cqrs/commands-endpoints"))
    .settings(
      noPublishSettings,
      `scala 2.12 to 2.13`,
      libraryDependencies ++= Seq(
        "org.scala-stm" %% "scala-stm" % "0.11.1",
        "io.circe" %% "circe-generic" % circeVersion
      )
    )
    .dependsOn(`algebra-circe-jvm`)

// commands implementation
val `example-cqrs-commands` =
  project
    .in(file("examples/cqrs/commands"))
    .settings(
      noPublishSettings,
      `scala 2.12 to 2.13`,
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
        "org.http4s" %% "http4s-blaze-server" % http4sVersion % Test,
        "org.http4s" %% "http4s-async-http-client" % http4sVersion % Test,
        "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test,
        scalaTestDependency
      )
    )
    .dependsOn(`http4s-server`, `http4s-client-jvm` % Test)
    .dependsOn(`example-cqrs-commands-endpoints`)

// queries endpoints definitions
lazy val `example-cqrs-queries-endpoints` =
  project
    .in(file("examples/cqrs/queries-endpoints"))
    .settings(noPublishSettings, `scala 2.12 to dotty`)
    .dependsOn(
      `algebra-circe-jvm`,
      `example-cqrs-public-endpoints-jvm` /* because we reuse the DTOs */
    )

// queries implementation
val `example-cqrs-queries` =
  project
    .in(file("examples/cqrs/queries"))
    .settings(noPublishSettings, `scala 2.12 to 2.13`)
    .dependsOn(`http4s-server`, `http4s-client-jvm`)
    .dependsOn(
      `example-cqrs-queries-endpoints`,
      `example-cqrs-commands-endpoints`
    )

// this one exists only for the sake of simplifying the infrastructure: it runs all the HTTP services
val `example-cqrs` =
  project
    .in(file("examples/cqrs/infra"))
    .settings(noPublishSettings, `scala 2.12 to 2.13`)
    .settings(
      Global / cancelable := true,
      libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-blaze-server" % http4sVersion,
        "org.http4s" %% "http4s-async-http-client" % http4sVersion,
        "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test,
        "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
        scalaTestDependency
      )
    )
    .dependsOn(
      `example-cqrs-queries`,
      `example-cqrs-commands`,
      `example-cqrs-public-server`
    )

val `example-documented` =
  project
    .in(file("examples/documented"))
    .settings(noPublishSettings, `scala 2.12 to dotty`)
    .settings(
      libraryDependencies += "org.http4s" %% "http4s-blaze-server" % http4sVersion
    )
    .dependsOn(`http4s-server`, `json-schema-generic-jvm`)

val `example-authentication` =
  project
    .in(file("examples/authentication"))
    .settings(noPublishSettings, `scala 2.12 to 2.13`)
    .settings(
      libraryDependencies ++= Seq(
        "com.github.jwt-scala" %% "jwt-circe" % "9.4.5",
        "org.http4s" %% "http4s-blaze-server" % http4sVersion % Test,
        "org.http4s" %% "http4s-async-http-client" % http4sVersion % Test,
        "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test,
        scalaTestDependency
      )
    )
    .dependsOn(`http4s-server`, `http4s-client-jvm`)

val `example-basic-http4s-server` =
  project
    .in(file("examples/basic/http4s-server"))
    .settings(
      noPublishSettings,
      `scala 2.12 to 2.13`,
      publishArtifact := false,
      libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.17",
      libraryDependencies += "org.http4s" %%% "http4s-blaze-server" % http4sVersion,
      libraryDependencies += "org.http4s" %% "http4s-circe" % http4sVersion
    )
    .dependsOn(`example-basic-shared-jvm`, `http4s-server`)
