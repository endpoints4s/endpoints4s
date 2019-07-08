import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `algebra-playjson-jvm` = LocalProject("algebra-playjsonJVM")
val `json-schema-circe-jvm` = LocalProject("json-schema-circeJVM")
val `json-schema-generic-jvm` = LocalProject("json-schema-genericJVM")
val `json-schema-playjson-jvm` = LocalProject("json-schema-playjsonJVM")

val akkaActorVersion = "2.5.22"
val akkaHttpVersion = "10.1.8"
val akkaHttpCirceVersion = "1.25.2"
val akkaHttpPlayJsonVersion = "1.27.0"

val `akka-http-client` =
  project.in(file("client"))
    .settings(
      publishSettings,
      `scala 2.11 to 2.12`,
      name := "endpoints-akka-http-client",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-stream" % akkaActorVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
        scalaTestDependency
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`algebra-circe-jvm` % "test->test")
    .dependsOn(`json-schema-circe-jvm`)
    .dependsOn(`json-schema-generic-jvm` % "test->test")

val `akka-http-server` =
  project.in(file("server"))
    .settings(
      publishSettings,
      `scala 2.11 to 2.12`,
      name := "endpoints-akka-http-server",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-stream" % akkaActorVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
        "com.typesafe.akka" %% "akka-testkit" % akkaActorVersion % Test,
        "com.softwaremill.sttp" %% "core" % sttpVersion % Test, // Temporary
        scalaTestDependency
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`json-schema-generic-jvm` % "test->test")

val `akka-http-server-circe` =
  project.in(file("server-circe"))
    .settings(
      publishSettings,
      `scala 2.11 to 2.12`,
      name := "endpoints-akka-http-server-circe",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-stream" % akkaActorVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
        "com.typesafe.akka" %% "akka-testkit" % akkaActorVersion % Test,
        "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion % Provided,
        "com.softwaremill.sttp" %% "core" % sttpVersion % Test, // Temporary
        scalaTestDependency
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`algebra-circe-jvm` % "test->test")
    .dependsOn(`akka-http-server` % "test->test")
    .dependsOn(`json-schema-circe-jvm`)
    .dependsOn(`json-schema-generic-jvm` % "test->test")
    .dependsOn(`akka-http-server`, `algebra-circe-jvm`, `json-schema-circe-jvm`)

val `akka-http-server-playjson` =
  project.in(file("server-playjson"))
    .settings(
      publishSettings,
      `scala 2.11 to 2.12`,
      name := "endpoints-akka-http-server-playjson",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-stream" % akkaActorVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
        "com.typesafe.akka" %% "akka-testkit" % akkaActorVersion % Test,
        "de.heikoseeberger" %% "akka-http-play-json" % akkaHttpPlayJsonVersion,
        "com.typesafe.play" %% "play-json" % playjsonVersion,
        "com.softwaremill.sttp" %% "core" % sttpVersion % Test, // Temporary
        scalaTestDependency
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`algebra-playjson-jvm` % "test->test")
    .dependsOn(`akka-http-server` % "test->test")
    .dependsOn(`json-schema-playjson-jvm`)
    .dependsOn(`json-schema-generic-jvm` % "test->test")
    .dependsOn(`akka-http-server`, `algebra-playjson-jvm`, `json-schema-playjson-jvm`)

