import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `testsuite-jvm` = LocalProject("testsuiteJVM")

val akkaHttpVersion = "10.0.1"
val akkaHttpJsonVersion = "1.18.1"

val `akka-http-client` =
  project.in(file("client"))
    .settings(publishSettings: _*)
    .settings(`scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-akka-http-client",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
        "org.scalatest" %% "scalatest" % "3.0.1" % Test
      )
    )
    .dependsOn(`algebra-jvm`, `testsuite-jvm` % Test)

val `akka-http-client-circe` =
  project.in(file("client-circe"))
    .settings(publishSettings: _*)
    .settings(`scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-akka-http-client-circe",
      libraryDependencies ++= Seq(
        "io.circe" %% "circe-parser" % circeVersion
      )
    )
    .dependsOn(`akka-http-client`, `algebra-circe-jvm`)

val `akka-http-server` =
  project.in(file("server"))
    .settings(publishSettings: _*)
    .settings(`scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-akka-http-server",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
        "org.scalatest" %% "scalatest" % "3.0.1" % Test
      )
    )
    .dependsOn(`algebra-jvm`)

val `akka-http-server-circe` =
  project.in(file("server-circe"))
    .settings(publishSettings: _*)
    .settings(`scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-akka-http-server-circe",
      libraryDependencies ++= Seq(
        "de.heikoseeberger" %% "akka-http-circe" % akkaHttpJsonVersion
      )
    )
    .dependsOn(`akka-http-server`, `algebra-circe-jvm`)
