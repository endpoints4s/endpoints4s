import EndpointsSettings._
import xerial.sbt.Sonatype.GitHubHosting

// Algebra interfaces
val algebras = project.in(file("algebras")).settings(noPublishSettings)
val jsonSchema = project.in(file("json-schema")).settings(noPublishSettings)

// Interpreters
val openapi = project.in(file("openapi")).settings(noPublishSettings)
val xhr = project.in(file("xhr")).settings(noPublishSettings)
val fetch = project.in(file("fetch")).settings(noPublishSettings)
val play = project.in(file("play")).settings(noPublishSettings)
val `akka-http` = project.in(file("akka-http")).settings(noPublishSettings)
val scalaj = project.in(file("scalaj")).settings(noPublishSettings)
val sttp = project.in(file("sttp")).settings(noPublishSettings)
val http4s = project.in(file("http4s")).settings(noPublishSettings)

// Documentation and examples
val documentation =
  project.in(file("documentation")).settings(noPublishSettings)

// Stub server for client interpreter tests
val stubServer = project.in(file("stub-server")).settings(noPublishSettings)

noPublishSettings

ThisBuild / ivyLoggingLevel := UpdateLogging.Quiet

ThisBuild / publishTo := sonatypePublishTo.value

Global / onChangedBuildSource := ReloadOnSourceChanges

Global / excludeLintKeys += coverageEnabled

ThisBuild / sonatypeProjectHosting := Some(
  GitHubHosting("endpoints4s", "endpoints4s", "julien@richard-foy.fr")
)

// Default intention: binary compatibility between releases.
// We want to keep binary compatibility as long as we can for the algebra,
// but it is OK to publish breaking releases of interpreters. So,
// interpreter modules may override this setting.
ThisBuild / versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
// Ignore dependencies to modules with version like `1.2.3+n`
ThisBuild / versionPolicyIgnoredInternalDependencyVersions := Some("^\\d+\\.\\d+\\.\\d+\\+n".r)
// Default version, used by the algebra modules, and by the interpreters,
// unless they override it.
ThisBuild / version := "1.6.0+n"

ThisBuild / libraryDependencySchemes ++= Seq(
  "com.softwaremill.sttp.client3" %%% "core" % "semver-spec",
  "com.softwaremill.sttp.model" %%% "core" % "semver-spec",
  "com.typesafe.akka" %%% "akka-http" % "semver-spec",
  "com.typesafe.akka" %%% "akka-http-core" % "semver-spec",
  "com.typesafe.akka" %%% "akka-parsing" % "semver-spec",
  "org.log4s" %%% "log4s" % "semver-spec"
)
