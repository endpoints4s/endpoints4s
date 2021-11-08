import EndpointsSettings._

val `xhr-client` =
  project
    .in(file("client"))
    .enablePlugins(ScalaJSPlugin)
    .configure(_.disablePlugins(ScoverageSbtPlugin))
    .settings(
      publishSettings,
      `scala 2.12 to 2.13`,
      name := "xhr-client",
      version := "3.1.0+n",
      versionPolicyIntention := Compatibility.BinaryCompatible,
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "2.0.0",
        "org.scalatest" %%% "scalatest" % scalaTestVersion % Test
      )
    )
    .dependsOn(LocalProject("algebraJS") % "test->test;compile->compile")
    .dependsOn(LocalProject("openapiJS"))

val `xhr-client-faithful` =
  project
    .in(file("client-faithful"))
    .enablePlugins(ScalaJSPlugin)
    .configure(_.disablePlugins(ScoverageSbtPlugin))
    .settings(
      publishSettings,
      `scala 2.12 to 2.13`,
      name := "xhr-client-faithful",
      version := "3.1.0+n",
      versionPolicyIntention := Compatibility.BinaryCompatible,
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false,
      libraryDependencies += "org.julienrf" %%% "faithful" % "2.0.0"
    )
    .dependsOn(`xhr-client`)

val `xhr-client-circe` =
  project
    .in(file("client-circe"))
    .enablePlugins(ScalaJSPlugin)
    .configure(_.disablePlugins(ScoverageSbtPlugin))
    .settings(
      publishSettings,
      `scala 2.12 to 2.13`,
      name := "xhr-client-circe",
      version := "3.1.0+n",
      versionPolicyIntention := Compatibility.None,
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
