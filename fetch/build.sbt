import EndpointsSettings._
import com.typesafe.tools.mima.core.{ProblemFilters, DirectMissingMethodProblem}

val `fetch-client` =
  project
    .in(file("client"))
    .enablePlugins(ScalaJSPlugin)
    .configure(_.disablePlugins(ScoverageSbtPlugin))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "fetch-client",
      mimaBinaryIssueFilters ++= Seq(
        // Was private to Scala users
        ProblemFilters.exclude[DirectMissingMethodProblem](
          "endpoints4s.fetch.EndpointsSettings.this"
        )
      ),
      //disable coverage for scala.js: https://github.com/scoverage/scalac-scoverage-plugin/issues/196
      coverageEnabled := false,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "2.4.0",
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
    .dependsOn(LocalProject("algebraJS"))
    .dependsOn(LocalProject("openapiJS"))
    .dependsOn(LocalProject("algebra-testkitJS") % Test)
    .dependsOn(LocalProject("algebra-circe-testkitJS") % Test)
    .dependsOn(LocalProject("json-schema-genericJS") % Test)

val `fetch-client-circe` =
  project
    .in(file("client-circe"))
    .enablePlugins(ScalaJSPlugin)
    .configure(_.disablePlugins(ScoverageSbtPlugin))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "fetch-client-circe",
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
      `fetch-client` % "test->test;compile->compile",
      LocalProject("algebra-circeJS"),
      LocalProject("json-schema-circeJS")
    )
