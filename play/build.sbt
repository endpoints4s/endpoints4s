import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-testkit-jvm` = LocalProject("algebra-testkitJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `algebra-circe-testkit-jvm` = LocalProject("algebra-circe-testkitJVM")
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
      version := "3.1.0+n",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        ("com.typesafe.play" %% "play-netty-server" % playVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.play" %% "play-test" % playVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.play" %% "play-ahc-ws" % playVersion % Test).cross(CrossVersion.for3Use2_13),
        // Override transitive dependencies of Play
        ("com.typesafe.akka" %% "akka-slf4j" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-actor-typed" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-serialization-jackson" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13)
      ),
      excludeDependencies ++= {
        if (scalaBinaryVersion.value.startsWith("3")) {
          List(
            ExclusionRule("org.scala-lang.modules", "scala-xml_3"),
            ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13")
          )
        } else Nil
      }
    )
    .dependsOn(
      `algebra-jvm`,
      `openapi-jvm`,
      `algebra-testkit-jvm` % Test,
      `algebra-circe-testkit-jvm` % Test
    )

val `play-server-circe` =
  project
    .in(file("server-circe"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "play-server-circe",
      version := "3.1.0+n",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies += "io.circe" %% "circe-parser" % circeVersion
    )
    .dependsOn(`play-server`, `algebra-circe-jvm`, `json-schema-circe-jvm`)

val `play-client` =
  project
    .in(file("client"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "play-client",
      version := "3.1.0+n",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        ("com.typesafe.play" %% "play-ahc-ws" % playVersion).cross(CrossVersion.for3Use2_13),
        // Override transitive dependencies of Play
        ("com.typesafe.akka" %% "akka-slf4j" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-actor-typed" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-serialization-jackson" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13)
      ),
      excludeDependencies ++= {
        if (scalaBinaryVersion.value.startsWith("3")) {
          List(
            ExclusionRule("org.scala-lang.modules", "scala-xml_3"),
            ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13")
          )
        } else Nil
      }
    )
    .dependsOn(
      `algebra-jvm`,
      `openapi-jvm`,
      `algebra-testkit-jvm` % Test,
      `algebra-circe-testkit-jvm` % Test,
      `json-schema-generic-jvm` % Test
    )
