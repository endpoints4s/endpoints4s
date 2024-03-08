import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-testkit-jvm` = LocalProject("algebra-testkitJVM")
val `algebra-playjson-jvm` = LocalProject("algebra-playjsonJVM")
val `openapi-jvm` = LocalProject("openapiJVM")

val `sttp-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "sttp-client",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
        "com.softwaremill.sttp.client3" %% "httpclient-backend" % sttpVersion % Test
      ),
      // FIXME Why is this necessary?
      excludeDependencies ++= {
        if (scalaBinaryVersion.value.startsWith("3")) {
          List(
            ExclusionRule("com.softwaremill.sttp.client3", "core_2.13"),
            ExclusionRule("com.softwaremill.sttp.model", "core_2.13"),
            ExclusionRule("com.softwaremill.sttp.shared", "ws_2.13"),
            ExclusionRule("com.softwaremill.sttp.shared", "core_2.13")
          )
        } else Nil
      }
    )
    .dependsOn(
      `algebra-jvm`,
      `openapi-jvm`,
      `algebra-testkit-jvm` % Test,
      `algebra-playjson-jvm` % "test->test"
    )
