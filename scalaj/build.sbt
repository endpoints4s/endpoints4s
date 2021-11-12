import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `openapi-jvm` = LocalProject("openapiJVM")

val `scalaj-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "scalaj-client",
      version := "3.1.0+n",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        ("org.scalaj" %% "scalaj-http" % "2.4.2").cross(CrossVersion.for3Use2_13)
      )
    )
    .dependsOn(`openapi-jvm`)
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`algebra-circe-jvm` % "test->test")
