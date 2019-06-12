import EndpointsSettings._
import xerial.sbt.Sonatype.GitHubHosting

// Algebra interfaces
val algebras = project.in(file("algebras")).settings(noPublishSettings)
val jsonSchema = project.in(file("json-schema")).settings(noPublishSettings)

// Interpreters
val openapi = project.in(file("openapi")).settings(noPublishSettings)
val xhr = project.in(file("xhr")).settings(noPublishSettings)
val play = project.in(file("play")).settings(noPublishSettings)
val `akka-http` = project.in(file("akka-http")).settings(noPublishSettings)
val scalaj = project.in(file("scalaj")).settings(noPublishSettings)
val sttp = project.in(file("sttp")).settings(noPublishSettings)
val http4s = project.in(file("http4s")).settings(noPublishSettings)

// Documentation and examples
val documentation = project.in(file("documentation")).settings(noPublishSettings)

noPublishSettings

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet

publishTo in ThisBuild := sonatypePublishTo.value

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / sonatypeProjectHosting := Some(GitHubHosting("julienrf", "endpoints", "julien.richard-foy@epfl.ch"))
