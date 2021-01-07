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
val documentation =
  project.in(file("documentation")).settings(noPublishSettings)

noPublishSettings

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet

publishTo in ThisBuild := sonatypePublishTo.value

Global / onChangedBuildSource := ReloadOnSourceChanges

Global / excludeLintKeys += coverageEnabled

ThisBuild / sonatypeProjectHosting := Some(
  GitHubHosting("endpoints4s", "endpoints4s", "julien@richard-foy.fr")
)

ThisBuild / versionScheme := Some("early-semver")

// Default intention: binary compatibility between releases.
// We want to keep binary compatibility as long as we can for the algebra,
// but it is OK to publish breaking releases of interpreters. So,
// interpreter modules may override this setting.
ThisBuild / versionPolicyIntention := Compatibility.BinaryCompatible
// Default version, used by the algebra modules, and by the interpreters,
// unless they override it.
ThisBuild / version := "1.3.0"

val versionSchemes = Def.setting {
  Seq(
    "org.endpoints4s" %%% "*" % "early-semver",
    "org.typelevel" %%% "cats*" % "early-semver",
    "co.fs2" %%% "fs2*" % "early-semver",
    "com.typesafe.play" %%% "play-json" % "early-semver",
    "com.typesafe.play" %%% "play-functional" % "early-semver",
    "com.typesafe.akka" %% "akka*" % "early-semver",
    "org.scala-js" % "scalajs-*" % "early-semver",
    "org.scala-lang.modules" %% "*" % "early-semver",
    "io.netty" % "*" % "always" // These guys use an unconventional versioning scheme
  )
}

ThisBuild / evictionRules ++= versionSchemes.value
ThisBuild / versionPolicyDependencyRules ++= versionSchemes.value
ThisBuild / versionPolicyIgnored += "joda-time" % "joda-time"
