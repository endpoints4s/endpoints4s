import EndpointsSettings._

val `sttp-client` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      publishSettings,
      `scala 2.12 to 2.13`,
      name := "sttp-client",
      version := "2.0.1",
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp.client" %%% "core" % sttpVersion
      )
    )
    .jsSettings(
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp.client" %% "akka-http-backend" % sttpVersion % Test,
        "com.typesafe.akka" %% "akka-stream" % "2.6.10" % Test
      )
    )
    .jsConfigure(project =>
      project
        .dependsOn(LocalProject("algebraJS") % "compile->compile")
        .disablePlugins(ScoverageSbtPlugin)
    )
    .jvmConfigure(project =>
      project
//        .dependsOn(LocalProject("openapiJVM") % "test->test")
        .dependsOn(
          LocalProject("algebraJVM") % "compile->compile;test->test",
          LocalProject("algebra-playjsonJVM") % "test->test"
        )
    )

