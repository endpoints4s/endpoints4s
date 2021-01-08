import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `algebra-playjson-jvm` = LocalProject("algebra-playjsonJVM")
val `json-schema-circe-jvm` = LocalProject("json-schema-circeJVM")
val `json-schema-generic-jvm` = LocalProject("json-schema-genericJVM")
val `json-schema-playjson-jvm` = LocalProject("json-schema-playjsonJVM")
val `openapi-jvm` = LocalProject("openapiJVM")

val `akka-http-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "akka-http-client",
      version := "3.0.0",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        ("com.typesafe.akka" %% "akka-stream" % akkaActorVersion).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-http" % akkaHttpVersion).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-stream-testkit" % akkaActorVersion % Test).withDottyCompat(scalaVersion.value),
        scalaTestDependency.withDottyCompat(scalaVersion.value)
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`openapi-jvm`)
    .dependsOn(`algebra-circe-jvm` % "test->test")
    .dependsOn(`json-schema-generic-jvm` % "test->test")

val `akka-http-server` =
  project
    .in(file("server"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "akka-http-server",
      version := "4.0.0",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        ("com.typesafe.akka" %% "akka-http" % akkaHttpVersion).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-stream" % akkaActorVersion).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-stream-testkit" % akkaActorVersion % Test).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-testkit" % akkaActorVersion % Test).withDottyCompat(scalaVersion.value),
        ("com.softwaremill.sttp.client" %% "core" % sttpVersion % Test).withDottyCompat(scalaVersion.value), // Temporary
        scalaTestDependency.withDottyCompat(scalaVersion.value)
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile", `openapi-jvm`)
    .dependsOn(`algebra-circe-jvm` % "test->test")
    .dependsOn(`json-schema-generic-jvm` % "test->test")
