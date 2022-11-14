import EndpointsSettings._

val `stub-server` =
  project
    .in(file("."))
    .settings(
      publishSettings,
      `scala 2.13`,
      name := "stub-server",
      version := "1.1.1",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaActorVersion
      ),
      mainClass := Some("endpoints4s.stubserver.StubServer")
    )
