import EndpointsSettings._

val `xhr-client` =
  project
    .in(file("client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-xhr-client",
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "1.0.0",
        "org.scalatest" %%% "scalatest" % scalaTestVersion % Test
      )
    )
    .dependsOn(LocalProject("algebraJS") % "test->test;compile->compile")
    .dependsOn(LocalProject("openapiJS"))

val `xhr-client-faithful` =
  project
    .in(file("client-faithful"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-xhr-client-faithful",
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false,
      libraryDependencies += "org.julienrf" %%% "faithful" % "2.0.0"
    )
    .dependsOn(`xhr-client`)

val `xhr-client-circe` =
  project
    .in(file("client-circe"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-xhr-client-circe",
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false,
      libraryDependencies += "io.circe" %%% "circe-parser" % circeVersion,
      jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()
    )
    .dependsOn(
      `xhr-client` % "test->test;compile->compile",
      LocalProject("algebra-circeJS"),
      LocalProject("json-schema-circeJS")
    )
