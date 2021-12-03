import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `algebra-circe-testkit-jvm` = LocalProject("algebra-circe-testkitJVM")
val `algebra-testkit-jvm` = LocalProject("algebra-testkitJVM")
val `openapi-jvm` = LocalProject("openapiJVM")

val `scalaj-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "scalaj-client",
      version := "4.0.0+n",
      libraryDependencies ++= Seq(
        ("org.scalaj" %% "scalaj-http" % "2.4.2").cross(CrossVersion.for3Use2_13)
      )
    )
    .dependsOn(
      `algebra-jvm`,
      `openapi-jvm`,
      `algebra-circe-jvm` % Test,
      `algebra-testkit-jvm` % Test,
      `algebra-circe-testkit-jvm` % Test
    )
