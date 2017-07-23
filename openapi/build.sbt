import EndpointsSettings._
import LocalCrossProject._

val testsuite = LocalProject("testsuite")

val openapi =
  crossProject.crossType(CrossType.Pure).in(file("openapi"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-openapi",
      libraryDependencies += "io.circe" %%% "circe-core" % circeVersion
    )
    .jsConfigure(_.dependsOn(LocalProject("testsuiteJS") % Test))
    .jvmConfigure(_.dependsOn(LocalProject("testsuiteJVM") % Test))
    .dependsOnLocalCrossProjects("algebra")

val `openapi-js` = openapi.js
val `openapi-jvm` = openapi.jvm

val `openapi-circe` =
  crossProject.crossType(CrossType.Pure).in(file("circe"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-openapi-circe",
      libraryDependencies += "io.circe" %%% "circe-core" % circeVersion
    )
    .dependsOn(openapi)
    .jsConfigure(_.dependsOn(LocalProject("testsuiteJS") % Test))
    .jvmConfigure(_.dependsOn(LocalProject("testsuiteJVM") % Test))
    .dependsOnLocalCrossProjects("algebra-circe")

val `openapi-circe-js` = `openapi-circe`.js
val `openapi-circe-jvm` = `openapi-circe`.jvm
