import EndpointsSettings._
import LocalCrossProject._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-testkit-jvm` = LocalProject("algebra-testkitJVM")
val `algebra-circe-testkit-jvm` = LocalProject("algebra-circe-testkitJVM")
val `json-schema-circe-jvm` = LocalProject("json-schema-circeJVM")
val `openapi-jvm` = LocalProject("openapiJVM")

val `algebra-js` = LocalProject("algebraJS")
val `algebra-circe-testkit-js` = LocalProject("algebra-circe-testkitJS")
val `json-schema-circe-js` = LocalProject("json-schema-circeJS")
val `openapi-js` = LocalProject("openapiJS")

val `http4s-server` =
  project
    .in(file("server"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "http4s-server",
      version := "9.0.0",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-core" % http4sVersion,
        "org.http4s" %% "http4s-dsl" % http4sVersion,
        "org.http4s" %% "http4s-blaze-server" % http4sVersion % Test
      )
    )
    .dependsOn(
      `algebra-jvm`,
      `openapi-jvm`,
      `algebra-testkit-jvm` % Test,
      `algebra-circe-testkit-jvm` % Test
    )

val `http4s-client` =
  crossProject(JSPlatform, JVMPlatform)
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "http4s-client",
      version := "6.1.0",
      libraryDependencies ++= Seq(
        "org.http4s" %%% "http4s-client" % http4sVersion
      )
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-async-http-client" % http4sVersion % Test
      )
    )
    .jsSettings(
      coverageEnabled := false,
      libraryDependencies ++= Seq(
        "org.http4s" %%% "http4s-dom" % http4sDomVersion % Test
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
    .dependsOnLocalCrossProjects("algebra", "openapi")
    .dependsOnLocalCrossProjectsWithScope(
      "algebra-testkit" -> Test,
      "algebra-circe-testkit" -> Test
    )
