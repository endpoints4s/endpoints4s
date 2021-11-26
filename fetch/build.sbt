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
      ),
      //TODO needs to be headless for CI
      Test / jsEnv := new org.scalajs.jsenv.selenium.SeleniumJSEnv(
        new org.openqa.selenium.chrome.ChromeOptions().addArguments(
          // recommended options
          "--disable-gpu",
          "--window-size=1920,1200",
          "--ignore-certificate-errors",
          "--disable-extensions",
          "--no-sandbox",
          "--disable-dev-shm-usage"
        ),
        //FIXME only for development
        org.scalajs.jsenv.selenium.SeleniumJSEnv
          .Config()
          .withKeepAlive(true)
      )
    )
    .dependsOn(LocalProject("algebraJS") % "test->test;compile->compile")
    .dependsOn(LocalProject("openapiJS"))
    .dependsOn(LocalProject("algebra-circeJS") % "test->test")
    .dependsOn(LocalProject("json-schema-genericJS") % "test->test")
