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
      version := "4.1.0+n",
      versionPolicyIntention := Compatibility.BinaryCompatible,
      libraryDependencies ++= Seq(
        ("com.typesafe.akka" %% "akka-stream" % akkaActorVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-http" % akkaHttpVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-stream-testkit" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        scalaTestDependency
      ),
      excludeDependencies ++= {
        if (scalaBinaryVersion.value.startsWith("3")) {
          List(ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"))
        } else Nil
      },
      versionPolicyIgnored ++= Seq(
        // Was removed from akka-http https://github.com/akka/akka-http/pull/3849
        "com.twitter" % "hpack"
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
      version := "5.1.0+n",
      versionPolicyIntention := Compatibility.BinaryCompatible,
      libraryDependencies ++= Seq(
        ("com.typesafe.akka" %% "akka-http" % akkaHttpVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-stream" % akkaActorVersion).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-stream-testkit" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.akka" %% "akka-testkit" % akkaActorVersion % Test).cross(CrossVersion.for3Use2_13),
        ("com.softwaremill.sttp.client" %% "core" % "2.2.10" % Test).cross(CrossVersion.for3Use2_13), // Temporary
        scalaTestDependency.cross(CrossVersion.for3Use2_13)
      ),
      excludeDependencies ++= {
        if (scalaBinaryVersion.value.startsWith("3")) {
          List(
            ExclusionRule("org.scala-lang.modules", "scala-xml_3"),
            ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"),
            ExclusionRule("org.scalatest", "scalatest-featurespec_2.13"),
            ExclusionRule("org.scalatest", "scalatest-shouldmatchers_2.13"),
            ExclusionRule("org.scalatest", "scalatest-matchers-core_2.13"),
            ExclusionRule("org.scalatest", "scalatest-diagrams_2.13"),
            ExclusionRule("org.scalatest", "scalatest_2.13"),
            ExclusionRule("org.scalatest", "scalatest-core_2.13"),
            ExclusionRule("org.scalatest", "scalatest-refspec_2.13"),
            ExclusionRule("org.scalatest", "scalatest-funspec_2.13"),
            ExclusionRule("org.scalatest", "scalatest-freespec_2.13"),
            ExclusionRule("org.scalatest", "scalatest-propspec_2.13"),
            ExclusionRule("org.scalatest", "scalatest-flatspec_2.13"),
            ExclusionRule("org.scalatest", "scalatest-funsuite_2.13"),
            ExclusionRule("org.scalatest", "scalatest-wordspec_2.13"),
            ExclusionRule("org.scalatest", "scalatest-mustmatchers_2.13"),
            ExclusionRule("org.scalactic", "scalactic_2.13")
          )
        } else Nil
      },
      versionPolicyIgnored ++= Seq(
        // Was removed from akka-http https://github.com/akka/akka-http/pull/3849
        "com.twitter" % "hpack"
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile", `openapi-jvm`)
    .dependsOn(`algebra-circe-jvm` % "test->test")
    .dependsOn(`json-schema-generic-jvm` % "test->test")
