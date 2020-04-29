import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `json-schema-circe-jvm` = LocalProject("json-schema-circeJVM")
val `openapi-jvm` = LocalProject("openapiJVM")

val `http4s-server` =
  project
    .in(file("server"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-http4s-server",
      libraryDependencies ++= Seq(
        "org.http4s" %%% "http4s-core" % http4sVersion,
        "org.http4s" %% "http4s-dsl" % http4sVersion,
        "org.http4s" %% "http4s-blaze-server" % http4sVersion % Test
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`openapi-jvm`)

val `http4s-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-http4s-client",
      libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-client" % http4sVersion,
        "org.http4s" %% "http4s-async-http-client" % http4sVersion % Test
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`openapi-jvm`)
    .dependsOn(`algebra-circe-jvm` % "compile->test;test->test")