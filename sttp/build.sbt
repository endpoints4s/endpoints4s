import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-playjson-jvm` = LocalProject("algebra-playjsonJVM")
val `openapi-jvm` = LocalProject("openapiJVM")

val `sttp-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "sttp-client",
      version := "4.1.0+n",
      versionPolicyIntention := Compatibility.BinaryCompatible,
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
        ("com.softwaremill.sttp.client3" %% "akka-http-backend" % sttpVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-stream" % "2.6.15" % Test).cross(CrossVersion.for3Use2_13)
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
    .dependsOn(`openapi-jvm`)
    .dependsOn(
      `algebra-jvm` % "compile->compile;test->test",
      `algebra-playjson-jvm` % "test->test"
    )
