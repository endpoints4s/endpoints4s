import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")
val `testsuite-jvm` = LocalProject("testsuiteJVM")

val `play-server` =
  project.in(file("server"))
    .settings(publishSettings ++ `scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-play-server",
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-netty-server" % playVersion
      )
    )
    .dependsOn(`algebra-jvm`)

val `play-server-circe` =
  project.in(file("server-circe"))
    .settings(publishSettings ++ `scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-play-server-circe",
      libraryDependencies += "io.circe" %% "circe-parser" % circeVersion
    )
    .dependsOn(`play-server`, `algebra-circe-jvm`)

val `play-client` =
  project.in(file("client"))
      .settings(publishSettings ++ `scala 2.11 to 2.12`: _*)
      .settings(
        name := "endpoints-play-client",
        libraryDependencies += "com.typesafe.play" %% "play-ahc-ws" % playVersion
      )
      .dependsOn(`algebra-jvm`, `testsuite-jvm` % Test)
