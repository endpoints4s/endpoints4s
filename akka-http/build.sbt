import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")

val `akka-http-server` =
  project.in(file("server"))
    .settings(publishSettings: _*)
    .settings(`scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-akka-http-server",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http" % "10.0.1",
        "com.typesafe.akka" %% "akka-http-testkit" % "10.0.1" % Test,
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
        "de.heikoseeberger" %% "akka-http-circe" % "1.11.0"
      )
    )
    .dependsOn(`akka-http-server`, `algebra-circe-jvm`)
