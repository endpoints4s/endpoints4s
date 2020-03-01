import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `openapi-jvm` = LocalProject("openapiJVM")
val `json-schema-circe-jvm` = LocalProject("json-schema-circeJVM")
val `json-schema-playjson-jvm` = LocalProject("json-schema-playjsonJVM")

val `play-server` =
  project.in(file("server"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`, // Note that we could support 2.11. Only our tests use circe (which has dropped 2.11)
      name := "endpoints-play-server",
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-netty-server" % playVersion,
        "com.typesafe.play" %% "play-test" % playVersion % Test,
        "com.typesafe.play" %% "play-ahc-ws" % playVersion % Test
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`openapi-jvm`)
    .dependsOn(`algebra-circe-jvm` % "test->test")

val `play-server-circe` =
  project.in(file("server-circe"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-play-server-circe",
      libraryDependencies += "io.circe" %% "circe-parser" % circeVersion
    )
    .dependsOn(`play-server`, `algebra-circe-jvm`, `json-schema-circe-jvm`)

val `play-client` =
  project.in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-play-client",
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-ahc-ws" % playVersion
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile", `algebra-circe-jvm` % "compile->test;test->test")
    .dependsOn(`openapi-jvm`)
