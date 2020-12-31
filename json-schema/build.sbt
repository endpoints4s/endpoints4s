import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import EndpointsSettings._
import LocalCrossProject._

val `json-schema` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("json-schema"))
    .jsSettings(`scala 2.12 to 2.13`)
    .jvmSettings(`scala 2.12 to dotty`)
    .settings(
      publishSettings,
      name := "algebra-json-schema",
      addScalaTestCrossDependency,
      libraryDependencies ++= Seq(
        ("org.scala-lang.modules" %%% "scala-collection-compat" % "2.3.2").withDottyCompat(scalaVersion.value),
        ("org.scalacheck" %%% "scalacheck" % "1.15.2" % Test).withDottyCompat(scalaVersion.value)
      ),
      (Compile / boilerplateSource) := baseDirectory.value / ".." / "src" / "main" / "boilerplate"
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .jsConfigure(
      _.settings(
        libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.1.0" % Test
      ).disablePlugins(ScoverageSbtPlugin)
    )

val `json-schema-js` = `json-schema`.js
val `json-schema-jvm` = `json-schema`.jvm

lazy val `json-schema-generic` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("json-schema-generic"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`, // Only pretend to make sbt happy
      name := "json-schema-generic",
      libraryDependencies += ("com.chuusai" %%% "shapeless" % "2.3.3").withDottyCompat(scalaVersion.value),
      addScalaTestCrossDependency,
      (Test / boilerplateSource) := baseDirectory.value / ".." / "src" / "test" / "boilerplate"
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))
    .dependsOnLocalCrossProjects("json-schema")

lazy val `json-schema-generic-js` = `json-schema-generic`.js
lazy val `json-schema-generic-jvm` = `json-schema-generic`.jvm

lazy val `json-schema-circe` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("json-schema-circe"))
    .jsSettings(`scala 2.12 to 2.13`)
    .jvmSettings(`scala 2.12 to dotty`)
    .settings(
      publishSettings,
      name := "json-schema-circe",
      libraryDependencies += ("io.circe" %%% "circe-core" % circeVersion).withDottyCompat(scalaVersion.value),
      (Compile / boilerplateSource) := baseDirectory.value / ".." / "src" / "main" / "boilerplate"
    )
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .dependsOnLocalCrossProjects(
      "algebra-circe"
    ) // Needed only because of CirceCodec, but that class doesn’t depend on the algebra
    .dependsOnLocalCrossProjectsWithScope(
      "json-schema" -> "test->test;compile->compile"
    )

lazy val `json-schema-circe-js` = `json-schema-circe`.js
lazy val `json-schema-circe-jvm` = `json-schema-circe`.jvm

lazy val `json-schema-playjson` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("json-schema-playjson"))
    .jsSettings(`scala 2.12 to 2.13`)
    .jvmSettings(`scala 2.12 to dotty`)
    .settings(
      publishSettings,
      name := "json-schema-playjson",
      libraryDependencies += ("com.typesafe.play" %%% "play-json" % playjsonVersion).withDottyCompat(scalaVersion.value),
      (Compile / boilerplateSource) := baseDirectory.value / ".." / "src" / "main" / "boilerplate"
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))
    .dependsOnLocalCrossProjectsWithScope(
      "json-schema" -> "test->test;compile->compile"
    )

lazy val `json-schema-playjson-js` = `json-schema-playjson`.js
lazy val `json-schema-playjson-jvm` = `json-schema-playjson`.jvm
