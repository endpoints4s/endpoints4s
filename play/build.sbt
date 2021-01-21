import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `openapi-jvm` = LocalProject("openapiJVM")
val `json-schema-circe-jvm` = LocalProject("json-schema-circeJVM")
val `json-schema-generic-jvm` = LocalProject("json-schema-genericJVM")
val `json-schema-playjson-jvm` = LocalProject("json-schema-playjsonJVM")

val `play-server` =
  project
    .in(file("server"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`, // Note that we could support 2.11. Only our tests use circe (which has dropped 2.11)
      name := "play-server",
      version := "2.0.0",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        ("com.typesafe.play" %% "play-netty-server" % playVersion).withDottyCompat(scalaVersion.value),
        ("com.typesafe.play" %% "play-test" % playVersion % Test).withDottyCompat(scalaVersion.value),
        ("com.typesafe.play" %% "play-ahc-ws" % playVersion % Test).withDottyCompat(scalaVersion.value),
        // Override transitive dependencies of Play
        ("com.typesafe.akka" %% "akka-slf4j" % akkaActorVersion % Test).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-actor-typed" % akkaActorVersion % Test).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-serialization-jackson" % akkaActorVersion % Test).withDottyCompat(scalaVersion.value)
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`openapi-jvm`)
    .dependsOn(`algebra-circe-jvm` % "test->test")

val `play-server-circe` =
  project
    .in(file("server-circe"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "play-server-circe",
      version := "2.0.0",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies += ("io.circe" %% "circe-parser" % circeVersion).withDottyCompat(scalaVersion.value)
    )
    .dependsOn(`play-server`, `algebra-circe-jvm`, `json-schema-circe-jvm`)

val `play-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "play-client",
      version := "2.0.0",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        ("com.typesafe.play" %% "play-ahc-ws" % playVersion).withDottyCompat(scalaVersion.value),
        // Override transitive dependencies of Play
        ("com.typesafe.akka" %% "akka-slf4j" % akkaActorVersion % Test).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-actor-typed" % akkaActorVersion % Test).withDottyCompat(scalaVersion.value),
        ("com.typesafe.akka" %% "akka-serialization-jackson" % akkaActorVersion % Test).withDottyCompat(scalaVersion.value)
      )
    )
    .dependsOn(
      `algebra-jvm` % "test->test;compile->compile",
      `algebra-circe-jvm` % "test->test"
    )
    .dependsOn(`openapi-jvm`)
    .dependsOn(`json-schema-generic-jvm` % "test->test")
