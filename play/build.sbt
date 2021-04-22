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
      version := "2.0.0+n",
      versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
      libraryDependencies ++= Seq(
        ("com.typesafe.play" %% "play-netty-server" % playVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.play" %% "play-test" % playVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.play" %% "play-ahc-ws" % playVersion % Test).cross(CrossVersion.for3Use2_13),
        // Override transitive dependencies of Play
        ("com.typesafe.akka" %% "akka-slf4j" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-actor-typed" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-serialization-jackson" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13)
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
      version := "2.0.0+n",
      versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
      libraryDependencies += ("io.circe" %% "circe-parser" % circeVersion).cross(CrossVersion.for3Use2_13)
    )
    .dependsOn(`play-server`, `algebra-circe-jvm`, `json-schema-circe-jvm`)

val `play-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "play-client",
      version := "2.0.0+n",
      versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
      libraryDependencies ++= Seq(
        ("com.typesafe.play" %% "play-ahc-ws" % playVersion).cross(CrossVersion.for3Use2_13),
        // Override transitive dependencies of Play
        ("com.typesafe.akka" %% "akka-slf4j" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-actor-typed" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-serialization-jackson" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13)
      )
    )
    .dependsOn(
      `algebra-jvm` % "test->test;compile->compile",
      `algebra-circe-jvm` % "test->test"
    )
    .dependsOn(`openapi-jvm`)
    .dependsOn(`json-schema-generic-jvm` % "test->test")
