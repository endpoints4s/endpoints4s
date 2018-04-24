import EndpointsSettings._

// Algebra interfaces
val algebras = project.in(file("algebras"))
val openapi = project.in(file("openapi"))

// Interpreters
val xhr = project.in(file("xhr"))
val play = project.in(file("play"))
val `akka-http` = project.in(file("akka-http"))
val scalaj = project.in(file("scalaj"))

// Test kit
val testsuite = project.in(file("testsuite"))

// Documentation and examples
val documentation = project.in(file("documentation"))

import ReleaseTransformations._

noPublishSettings

releaseCrossBuild := false

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("+test"),
  setReleaseVersion,
  releaseStepTask(makeSite in LocalProject("; wow 2.11.12; manual")),
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand(Sonatype.SonatypeCommand.sonatypeReleaseAll),
  releaseStepCommandAndRemaining("; wow 2.11.12; manual/ghpagesPushSite"),
  pushChanges
)
