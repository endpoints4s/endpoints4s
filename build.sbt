import EndpointsSettings._

// Algebra interfaces
val algebras = project.in(file("algebras"))
val jsonSchema = project.in(file("json-schema"))

// Interpreters
val openapi = project.in(file("openapi"))
val xhr = project.in(file("xhr"))
val play = project.in(file("play"))
val `akka-http` = project.in(file("akka-http"))
val scalaj = project.in(file("scalaj"))
val sttp = project.in(file("sttp"))
val http4s = project.in(file("http4s"))

// Test kit
val testsuite = project.in(file("testsuite"))

// Documentation and examples
val documentation = project.in(file("documentation"))

noPublishSettings

enablePlugins(CrossPerProjectPlugin)

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet

inThisBuild(Seq(
  pgpPublicRing := file("ci/pubring.asc"),
  pgpSecretRing := file("ci/secring.asc"),
  pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toArray)
))

publishTo in ThisBuild := sonatypePublishTo.value
