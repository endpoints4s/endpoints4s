import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `json-schema-circe-jvm` = LocalProject("json-schema-circeJVM")

val `http4s-server` =
  project
    .in(file("server"))
    .settings(
      publishSettings,
      `scala 2.11 to 2.12`,
      name := "endpoints-http4s-server",
      libraryDependencies ++= Seq(
        "org.http4s" %%% "http4s-core" % http4sVersion,
        "org.http4s" %% "http4s-dsl" % http4sVersion
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")

val `http4s-server-circe` =
  project
    .in(file("server-circe"))
    .settings(
      publishSettings,
      `scala 2.11 to 2.12`,
      name := "endpoints-http4s-server-circe",
      libraryDependencies += "org.http4s" %% "http4s-circe" % http4sVersion
    )
    .dependsOn(`http4s-server`, `algebra-circe-jvm`, `json-schema-circe-jvm`)
