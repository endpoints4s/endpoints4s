import EndpointsSettings._

val `stub-server` =
  project
    .in(file("."))
    .settings(
      `scala 2.13`,
      name := "stub-server",
      version := "1.0.0",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaActorVersion
      ),
      mainClass := Some("endpoints4s.stubserver.StubServer")
    )
