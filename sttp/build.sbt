import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-playjson-jvm` = LocalProject("algebra-playjsonJVM")

val `sttp-client` =
  project.in(file("client"))
    .settings(publishSettings ++ `scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-sttp-client",
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp" %% "core" % sttpVersion,
        "com.softwaremill.sttp" %% "akka-http-backend" % sttpVersion % Test,
        "com.typesafe.akka" %% "akka-stream" % "2.5.18" % Test
      )
    )
    .dependsOn(`algebra-jvm` % "compile->compile;test->test", `algebra-playjson-jvm` % "test->test")
