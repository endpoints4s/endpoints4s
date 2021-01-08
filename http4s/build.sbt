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
      `scala 2.12 to dotty`,
      name := "http4s-server",
      version := "5.0.0",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        ("org.http4s" %% "http4s-core" % http4sVersion).withDottyCompat(scalaVersion.value),
        ("org.http4s" %% "http4s-dsl" % http4sVersion).withDottyCompat(scalaVersion.value),
        ("org.http4s" %% "http4s-blaze-server" % http4sVersion % Test).withDottyCompat(scalaVersion.value)
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`openapi-jvm`)

val `http4s-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "http4s-client",
      version := "3.0.0",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        ("org.http4s" %% "http4s-client" % http4sVersion).withDottyCompat(scalaVersion.value),
        ("org.http4s" %% "http4s-async-http-client" % http4sVersion % Test).withDottyCompat(scalaVersion.value)
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`openapi-jvm`)
    .dependsOn(`algebra-circe-jvm` % "test->compile;test->test")
