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
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
        ("com.softwaremill.sttp.client3" %% "pekko-http-backend" % sttpVersion % Test)
          .cross(CrossVersion.for3Use2_13),
        "org.apache.pekko" %% "pekko-stream" % pekkoActorVersion % Test
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
