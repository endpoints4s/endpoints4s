import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-playjson-jvm` = LocalProject("algebra-playjsonJVM")

val `sttp3-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to 2.13`,
      name := "sttp3-client",
      version := "3.0.0",
      versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp.client3" %% "core" % sttp3Version,
        "com.softwaremill.sttp.client3" %% "akka-http-backend" % sttp3Version % Test,
        "com.typesafe.akka" %% "akka-stream" % "2.6.14" % Test
      )
    )
    .dependsOn(LocalProject("openapiJVM"))
    .dependsOn(
      `algebra-jvm` % "compile->compile;test->test",
      `algebra-playjson-jvm` % "test->test"
    )
