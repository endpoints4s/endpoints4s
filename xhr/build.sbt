import EndpointsSettings._

val `xhr-client` =
  project
    .in(file("client"))
    .enablePlugins(ScalaJSPlugin)
    .configure(_.disablePlugins(ScoverageSbtPlugin))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "xhr-client",
      version := "4.0.0+n",
      versionPolicyIntention := Compatibility.None,
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "2.0.0",
        "org.scalatest" %%% "scalatest" % scalaTestVersion % Test
      ),
      Test / jsEnv := new org.scalajs.jsenv.selenium.SeleniumJSEnv(
        new org.openqa.selenium.chrome.ChromeOptions().addArguments(
          // recommended options
          "--headless", // necessary for CI
          "--disable-gpu",
          "--window-size=1920,1200",
          "--ignore-certificate-errors",
          "--disable-extensions",
          "--no-sandbox",
          "--disable-dev-shm-usage",
          "--disable-web-security" // for CORS
        )
        // useful for development
        //org.scalajs.jsenv.selenium.SeleniumJSEnv.Config().withKeepAlive(true)
      )
    )
    .dependsOn(
      LocalProject("algebraJS"),
      LocalProject("openapiJS"),
      LocalProject("algebra-testkitJS") % Test,
      LocalProject("algebra-circe-testkitJS") % Test,
      LocalProject("json-schema-genericJS") % Test
    )

val `xhr-client-faithful` =
  project
    .in(file("client-faithful"))
    .enablePlugins(ScalaJSPlugin)
    .configure(_.disablePlugins(ScoverageSbtPlugin))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "xhr-client-faithful",
      version := "4.0.0+n",
      versionPolicyIntention := Compatibility.None,
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false,
      libraryDependencies += ("org.julienrf" %%% "faithful" % "2.0.0")
        .cross(CrossVersion.for3Use2_13),
      Test / jsEnv := new org.scalajs.jsenv.selenium.SeleniumJSEnv(
        new org.openqa.selenium.chrome.ChromeOptions().addArguments(
          // recommended options
          "--headless", // necessary for CI
          "--disable-gpu",
          "--window-size=1920,1200",
          "--ignore-certificate-errors",
          "--disable-extensions",
          "--no-sandbox",
          "--disable-dev-shm-usage",
          "--disable-web-security" // for CORS
        )
        // useful for development
        //org.scalajs.jsenv.selenium.SeleniumJSEnv.Config().withKeepAlive(true)
      )
    )
    .dependsOn(`xhr-client`)

val `xhr-client-circe` =
  project
    .in(file("client-circe"))
    .enablePlugins(ScalaJSPlugin)
    .configure(_.disablePlugins(ScoverageSbtPlugin))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "xhr-client-circe",
      version := "4.0.0+n",
      versionPolicyIntention := Compatibility.None,
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false,
      libraryDependencies += "io.circe" %%% "circe-parser" % circeVersion,
      Test / jsEnv := new org.scalajs.jsenv.selenium.SeleniumJSEnv(
        new org.openqa.selenium.chrome.ChromeOptions().addArguments(
          // recommended options
          "--headless", // necessary for CI
          "--disable-gpu",
          "--window-size=1920,1200",
          "--ignore-certificate-errors",
          "--disable-extensions",
          "--no-sandbox",
          "--disable-dev-shm-usage",
          "--disable-web-security" // for CORS
        )
        // useful for development
        //org.scalajs.jsenv.selenium.SeleniumJSEnv.Config().withKeepAlive(true)
      )
    )
    .dependsOn(
      `xhr-client` % "test->test;compile->compile",
      LocalProject("algebra-circeJS"),
      LocalProject("json-schema-circeJS")
    )
