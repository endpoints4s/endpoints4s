import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-testkit-jvm` = LocalProject("algebra-testkitJVM")
val `algebra-pekko-testkit-jvm` = LocalProject("algebra-pekko-testkitJVM")
val `algebra-circe-testkit-jvm` = LocalProject("algebra-circe-testkitJVM")
val `json-schema-generic-jvm` = LocalProject("json-schema-genericJVM")
val `openapi-jvm` = LocalProject("openapiJVM")

val `pekko-http-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "pekko-http-client",
      publish / skip := scalaBinaryVersion.value.startsWith("3"),
      libraryDependencies ++= Seq(
        "org.apache.pekko" %% "pekko-stream" % pekkoActorVersion % Provided,
        "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
        "org.apache.pekko" %% "pekko-http-testkit" % pekkoHttpVersion % Test,
        "org.apache.pekko" %% "pekko-stream-testkit" % pekkoActorVersion % Test,
        scalaTestDependency
      ),
      excludeDependencies ++= {
        if (scalaBinaryVersion.value.startsWith("3")) {
          List(ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"))
        } else Nil
      }
    )
    .dependsOn(`algebra-jvm`)
    .dependsOn(`openapi-jvm`)
    .dependsOn(`algebra-pekko-testkit-jvm` % Test)
    .dependsOn(`algebra-circe-testkit-jvm` % Test)
    .dependsOn(`json-schema-generic-jvm` % "test->test")

val `pekko-http-server` =
  project
    .in(file("server"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "pekko-http-server",
      publish / skip := scalaBinaryVersion.value.startsWith("3"),
      libraryDependencies ++= Seq(
        "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
        "org.apache.pekko" %% "pekko-stream" % pekkoActorVersion % Provided,
        "org.apache.pekko" %% "pekko-http-testkit" % pekkoHttpVersion % Test,
        "org.apache.pekko" %% "pekko-stream-testkit" % pekkoActorVersion % Test,
        "org.apache.pekko" %% "pekko-testkit" % pekkoActorVersion % Test,
        scalaTestDependency
      ),
      excludeDependencies ++= {
        if (scalaBinaryVersion.value.startsWith("3")) {
          List(
            ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13")
          )
        } else Nil
      },
      versionPolicyIgnored ++= Seq(
        // Was removed from akka-http https://github.com/akka/akka-http/pull/3849
        "com.twitter" % "hpack"
      )
    )
    .dependsOn(`algebra-jvm`, `openapi-jvm`)
    .dependsOn(`algebra-testkit-jvm` % Test)
    .dependsOn(`algebra-circe-testkit-jvm` % Test)
    .dependsOn(`json-schema-generic-jvm` % "test->test")
