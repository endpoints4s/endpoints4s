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
      version := "3.0.0+n",
      versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
      libraryDependencies ++= Seq(
        ("com.typesafe.akka" %% "akka-stream" % akkaActorVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-http" % akkaHttpVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-stream-testkit" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        scalaTestDependency.cross(CrossVersion.for3Use2_13)
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
      version := "4.0.0+n",
      versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
      libraryDependencies ++= Seq(
        ("com.typesafe.akka" %% "akka-http" % akkaHttpVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-stream" % akkaActorVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-stream-testkit" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-testkit" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.softwaremill.sttp.client" %% "core" % sttpVersion % Test).cross(CrossVersion.for3Use2_13), // Temporary
        scalaTestDependency.cross(CrossVersion.for3Use2_13)
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile", `openapi-jvm`)
    .dependsOn(`algebra-circe-jvm` % "test->test")
    .dependsOn(`json-schema-generic-jvm` % "test->test")
