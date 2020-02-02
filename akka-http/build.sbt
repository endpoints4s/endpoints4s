import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `algebra-playjson-jvm` = LocalProject("algebra-playjsonJVM")
val `json-schema-circe-jvm` = LocalProject("json-schema-circeJVM")
val `json-schema-generic-jvm` = LocalProject("json-schema-genericJVM")
val `json-schema-playjson-jvm` = LocalProject("json-schema-playjsonJVM")
val `openapi-jvm` = LocalProject("openapiJVM")

val `akka-http-client` =
  project.in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`, // Note that we could support 2.11, only our tests depend on circe (which has dropped 2.11 support)
      name := "endpoints-akka-http-client",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-stream" % akkaActorVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
        "com.typesafe.akka" %% "akka-stream-testkit" % akkaActorVersion % Test,
        scalaTestDependency
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`openapi-jvm`)
    .dependsOn(`algebra-circe-jvm` % "test->test")
    .dependsOn(`json-schema-generic-jvm` % "test->test")

val `akka-http-server` =
  project.in(file("server"))
    .settings(
      publishSettings,
      `scala 2.12 to latest`,
      name := "endpoints-akka-http-server",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaActorVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
        "com.typesafe.akka" %% "akka-stream-testkit" % akkaActorVersion % Test,
        "com.typesafe.akka" %% "akka-testkit" % akkaActorVersion % Test,
        "com.softwaremill.sttp" %% "core" % sttpVersion % Test, // Temporary
        scalaTestDependency
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile", `openapi-jvm`)
    .dependsOn(`algebra-circe-jvm` % "test->test")
    .dependsOn(`json-schema-generic-jvm` % "test->test")
