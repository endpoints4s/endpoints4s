import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `json-schema-circe-jvm` = LocalProject("json-schema-circeJVM")
val `openapi-jvm` = LocalProject("openapiJVM")

val `algebra-js` = LocalProject("algebraJS")
val `algebra-circe-js` = LocalProject("algebra-circeJS")
val `json-schema-circe-js` = LocalProject("json-schema-circeJS")
val `openapi-js` = LocalProject("openapiJS")

val `http4s-server` =
  project
    .in(file("server"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "http4s-server",
      versionPolicyIntention := Compatibility.None,
      version := "7.0.0+n",
      libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-core" % http4sVersion,
        "org.http4s" %% "http4s-dsl" % http4sVersion,
        "org.http4s" %% "http4s-blaze-server" % http4sVersion % Test
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`openapi-jvm`)
    .dependsOn(`algebra-circe-jvm` % "test->compile;test->test")

val `http4s-client` =
  crossProject(JSPlatform, JVMPlatform)
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "http4s-client",
      versionPolicyIntention := Compatibility.None,
      version := "5.0.0+n",
      libraryDependencies ++= Seq(
        "org.http4s" %%% "http4s-client" % http4sVersion
      )
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-async-http-client" % http4sVersion % Test
      )
    )
    .jvmConfigure(
      _.dependsOn(`algebra-jvm` % "test->test;compile->compile")
        .dependsOn(`openapi-jvm`)
        .dependsOn(`algebra-circe-jvm` % "test->compile;test->test")
    )
    .jsConfigure(
      _.dependsOn(`algebra-js` % "test->test;compile->compile")
        .dependsOn(`openapi-js`)
        .dependsOn(`algebra-circe-js` % "test->compile;test->test")
    )
