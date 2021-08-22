import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-playjson-jvm` = LocalProject("algebra-playjsonJVM")

val `sttp-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to 2.13`,
      name := "sttp-client",
      version := "4.1.0+n",
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
        "com.softwaremill.sttp.client3" %% "akka-http-backend" % sttpVersion % Test,
        "com.typesafe.akka" %% "akka-stream" % "2.6.15" % Test
      )
    )
    .dependsOn(LocalProject("openapiJVM"))
    .dependsOn(
      `algebra-jvm` % "compile->compile;test->test",
      `algebra-playjson-jvm` % "test->test"
    )
