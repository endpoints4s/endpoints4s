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
      version := "7.0.0+n",
      versionPolicyIntention := Compatibility.BinaryCompatible,
      libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-core" % http4sVersion,
        "org.http4s" %% "http4s-dsl" % http4sVersion,
        "org.http4s" %% "http4s-blaze-server" % http4sVersion % Test
      ),
      excludeDependencies ++= {
        if (scalaBinaryVersion.value.startsWith("3")) {
          List(ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"))
        } else Nil
      }
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
      version := "5.0.0+n",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        "org.http4s" %%% "http4s-client" % "0.23.6"
      ),
      excludeDependencies ++= {
        if (scalaBinaryVersion.value.startsWith("3")) {
          List(ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"))
        } else Nil
      }
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
