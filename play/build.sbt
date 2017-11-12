import EndpointsSettings._

val `algebra-jvm` = LocalProject("algebraJVM")
val `algebra-circe-jvm` = LocalProject("algebra-circeJVM")

val `play-circe` =
  project.in(file("circe"))
    .settings(publishSettings ++ `scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-play-circe",
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play" % playVersion,
        "io.circe" %% "circe-core" % circeVersion
      )
    )

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
      libraryDependencies += "io.circe" %% "circe-jawn" % circeVersion
    )
    .dependsOn(`play-server`, `algebra-circe-jvm`, `play-circe`)

val `play-client` =
  project.in(file("client"))
      .settings(publishSettings ++ `scala 2.11 to 2.12`: _*)
      .settings(
        name := "endpoints-play-client",
        libraryDependencies += "com.typesafe.play" %% "play-ahc-ws" % playVersion
      )
      .dependsOn(`algebra-jvm`)

val `play-client-circe` =
  project.in(file("client-circe"))
    .settings(publishSettings ++ `scala 2.11 to 2.12`: _*)
    .settings(
      name := "endpoints-play-client-circe",
      libraryDependencies += "io.circe" %% "circe-jawn" % circeVersion
    )
    .dependsOn(`play-client`, `algebra-circe-jvm`, `play-circe`)

