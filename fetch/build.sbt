import EndpointsSettings._

val `fetch-client` =
  project
    .in(file("client"))
    .enablePlugins(ScalaJSPlugin)
    .configure(_.disablePlugins(ScoverageSbtPlugin))
    .settings(
      publishSettings,
      `scala 2.12 to 2.13`,
      name := "fetch-client",
      version := "3.1.0+n",
      versionPolicyIntention := Compatibility.BinaryCompatible,
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "1.2.0",
        "org.scalatest" %%% "scalatest" % scalaTestVersion % Test
      )
    )
    .dependsOn(LocalProject("algebraJS") % "test->test;compile->compile")
    .dependsOn(LocalProject("openapiJS"))
    .dependsOn(LocalProject("algebra-circeJS") % "test->test")
    .dependsOn(LocalProject("json-schema-genericJS") % "test->test")
