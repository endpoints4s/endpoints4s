import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-playjson-jvm` = LocalProject("algebra-playjsonJVM")

val `sttp-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "sttp-client",
      version := "3.0.0+n",
      versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
      libraryDependencies ++= Seq(
        ("com.softwaremill.sttp.client" %% "core" % sttpVersion).cross(CrossVersion.for3Use2_13),
        ("com.softwaremill.sttp.client" %% "akka-http-backend" % sttpVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-stream" % "2.6.14" % Test).cross(CrossVersion.for3Use2_13)
      )
    )
    .dependsOn(LocalProject("openapiJVM"))
    .dependsOn(
      `algebra-jvm` % "compile->compile;test->test",
      `algebra-playjson-jvm` % "test->test"
    )
