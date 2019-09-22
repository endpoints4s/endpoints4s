import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")

val `scalaj-client` =
  project.in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-scalaj-client",
      libraryDependencies ++= Seq(
        "org.scalaj" %% "scalaj-http" % "2.4.2"
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`algebra-circe-jvm` % "test->test")
