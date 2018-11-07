import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `algebra-playjson-jvm` = LocalProject("algebra-playjsonJVM")
val `json-schema-circe-jvm` = LocalProject("json-schema-circeJVM")
val `json-schema-generic-jvm` = LocalProject("json-schema-genericJVM")

val akkaHttpVersion = "10.0.14"
val akkaHttpJsonVersion = "1.18.1"
val akkaHttpCirceVersion = "1.22.0"

val `akka-http-client` =
  project.in(file("client"))
    .settings(publishSettings: _*)
    .settings(`scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-akka-http-client",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
        scalaTestDependency
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`algebra-circe-jvm` % "test->test")

val `akka-http-server` =
  project.in(file("server"))
    .settings(publishSettings: _*)
    .settings(`scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-akka-http-server",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
        "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion % Provided,
        "com.softwaremill.sttp" %% "core" % sttpVersion % Test, // Temporary
        scalaTestDependency
      )
    )
    .dependsOn(`algebra-jvm` % "test->test;compile->compile")
    .dependsOn(`algebra-circe-jvm` % "test->test")
    .dependsOn(`json-schema-circe-jvm`)
    .dependsOn(`json-schema-generic-jvm` % "test->test")
