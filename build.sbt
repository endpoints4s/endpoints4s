import EndpointsSettings._

// Algebra interfaces
val algebras = project.in(file("algebras"))
val jsonSchema = project.in(file("json-schema"))
val openapi = project.in(file("openapi"))

// Interpreters
val xhr = project.in(file("xhr"))
val play = project.in(file("play"))
val `akka-http` = project.in(file("akka-http"))
val scalaj = project.in(file("scalaj"))
val sttp = project.in(file("sttp"))

// Test kit
val testsuite = project.in(file("testsuite"))

// Documentation and examples
val documentation = project.in(file("documentation"))

import ReleaseTransformations._

noPublishSettings

enablePlugins(CrossPerProjectPlugin)

releaseCrossBuild := false

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("+test"),
  setReleaseVersion,
  releaseStepCommandAndRemaining("; wow 2.11.12; manual/makeSite"),
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand(Sonatype.SonatypeCommand.sonatypeReleaseAll),
  releaseStepCommandAndRemaining("; wow 2.11.12; manual/ghpagesPushSite"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
