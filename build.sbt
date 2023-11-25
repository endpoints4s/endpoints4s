import EndpointsSettings._
import xerial.sbt.Sonatype.GitHubHosting

// Algebra interfaces
val algebras = project.in(file("algebras")).settings(noPublishSettings)
val jsonSchema = project.in(file("json-schema")).settings(noPublishSettings)

// Interpreters
val openapi = project.in(file("openapi")).settings(noPublishSettings)
val fetch = project.in(file("fetch")).settings(noPublishSettings)
val `pekko-http` = project.in(file("pekko-http")).settings(noPublishSettings)
val sttp = project.in(file("sttp")).settings(noPublishSettings)
val http4s = project.in(file("http4s")).settings(noPublishSettings)

// Documentation and examples
val documentation =
  project.in(file("documentation")).settings(noPublishSettings)

// Stub server for client interpreter tests
val stubServer = project.in(file("stub-server")).settings(noPublishSettings)

noPublishSettings

ThisBuild / ivyLoggingLevel := UpdateLogging.Quiet

ThisBuild / publishTo := sonatypePublishToBundle.value

Global / onChangedBuildSource := ReloadOnSourceChanges

Global / excludeLintKeys += coverageEnabled

ThisBuild / sonatypeProjectHosting := Some(
  GitHubHosting("endpoints4s", "endpoints4s", "julien@richard-foy.fr")
)

// Set the default version so that it is pushed as a tag by sbt-release.
ThisBuild / version := (LocalProject("algebraJVM") / version).value

// Default intention: binary compatibility between releases.
// We want to keep binary compatibility as long as we can for the algebra,
// but it is OK to publish breaking releases of interpreters. So,
// interpreter modules may override this setting.
ThisBuild / versionPolicyIntention := Compatibility.BinaryCompatible
// Ignore dependencies to modules with version like `1.2.3+n`
ThisBuild / versionPolicyIgnoredInternalDependencyVersions := Some("^\\d+\\.\\d+\\.\\d+\\+n".r)

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.log4s" %%% "log4s" % "semver-spec"
)

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  tagRelease,
  pushChanges
)

import com.typesafe.tools.mima.core._
ThisBuild / mimaBinaryIssueFilters ++= Seq(
  // OK, constructor is private (but accessed from within the companion)
  ProblemFilters.exclude[DirectMissingMethodProblem]("endpoints4s.openapi.model.OpenApi.this")
)
