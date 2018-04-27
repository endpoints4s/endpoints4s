import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `testsuite-jvm` = LocalProject("testsuiteJVM")

val sttpVersion = "1.1.13"

val `sttp-client` =
  project.in(file("client"))
    .settings(publishSettings ++ `scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-sttp-client",
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp" %% "core" % sttpVersion,
        "com.softwaremill.sttp" %% "akka-http-backend" % sttpVersion % Test,
        "com.typesafe.akka" %% "akka-stream" % "2.5.11" % Test
      )
    )
    .dependsOn(`algebra-jvm`, `testsuite-jvm` % Test)
