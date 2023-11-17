import EndpointsSettings._

val `stub-server` =
  project
    .in(file("."))
    .settings(
      publishSettings,
      `scala 2.13`,
      name := "stub-server",
      versionPolicyIntention := Compatibility.None,
      libraryDependencies ++= Seq(
        "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
        "org.apache.pekko" %% "pekko-stream" % pekkoActorVersion
      )
    )
