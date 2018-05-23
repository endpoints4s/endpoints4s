import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")

val `scalaj-client` =
  project.in(file("client"))
    .settings(publishSettings: _*)
    .settings(`scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-scalaj-client",
      libraryDependencies ++= Seq(
        "org.scalaj" %% "scalaj-http" % "2.3.0"
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`algebra-circe-jvm` % "test->test")
