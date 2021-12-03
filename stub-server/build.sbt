import EndpointsSettings._
import com.typesafe.sbt.packager.docker.DockerAlias
import com.typesafe.sbt.packager.docker.ExecCmd

val `stub-server` =
  project
    .in(file("."))
    .enablePlugins(JavaAppPackaging, DockerPlugin)
    .settings(
      publishSettings,
      `scala 2.13`,
      name := "stub-server",
      version := "1.0.0+n",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaActorVersion
      ),
      mainClass := Some("endpoints4s.stubserver.StubServer"),
      Docker / dockerAliases += DockerAlias(None, None, "stubserver", None),
      Docker / packageName := "stubserver",
      dockerBaseImage := "openjdk:8-alpine",
      dockerCommands := {
        val (stage0, stage1) = dockerCommands.value.splitAt(8)
        val (stage1part1, stage1part2) = stage1.splitAt(5)
        stage0 ++ stage1part1 ++ Seq(
          ExecCmd("RUN", "apk", "add", "--no-cache", "bash")
        ) ++ stage1part2
      },
      dockerExposedPorts ++= Seq(8080)
    )
