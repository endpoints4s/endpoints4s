import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-testkit-jvm` = LocalProject("algebra-testkitJVM")
val `algebra-circe-testkit-jvm` = LocalProject("algebra-circe-testkitJVM")
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
      version := "5.2.0+n",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-stream" % akkaActorVersion % Provided,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
        "com.typesafe.akka" %% "akka-stream-testkit" % akkaActorVersion % Test,
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
    .dependsOn(`algebra-testkit-jvm` % Test)
    .dependsOn(`algebra-circe-testkit-jvm` % Test)
    .dependsOn(`json-schema-generic-jvm` % "test->test")

val `akka-http-server` =
  project
    .in(file("server"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "akka-http-server",
      version := "7.0.0+n",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaActorVersion % Provided,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
        "com.typesafe.akka" %% "akka-stream-testkit" % akkaActorVersion % Test,
        "com.typesafe.akka" %% "akka-testkit" % akkaActorVersion % Test,
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
