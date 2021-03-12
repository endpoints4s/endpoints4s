import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import EndpointsSettings._
import LocalCrossProject._

val `json-schema` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("json-schema"))
    .settings(
      `scala 2.12 to dotty`,
      publishSettings,
      name := "algebra-json-schema",
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %%% "scala-collection-compat" % "2.4.4",
        "org.scalacheck" %%% "scalacheck" % "1.15.4" % Test,
        scalaTestDependency
      ),
      (Compile / boilerplateSource) := baseDirectory.value / ".." / "src" / "main" / "boilerplate"
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .jsConfigure(
      _.settings(
        libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.3.0" % Test
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
      libraryDependencies ++= {
        val commonDependencies = Seq(scalaTestDependency)
        val shapelessDependency =
          if (scalaVersion.value.startsWith("2.")) "com.chuusai" %%% "shapeless" % "2.3.7"
          else "org.typelevel" %% "shapeless3-deriving" % "3.0.1"
        shapelessDependency +: commonDependencies
      },
      (Test / boilerplateSource) := baseDirectory.value / ".." / "src" / "test" / "boilerplate",
      // Add src/main/scala-2 as a source directory for Scala 2.12 and 2.13
      (Compile / unmanagedSourceDirectories) ++= {
        val crossSourceDirectory = baseDirectory.value / ".." / "src" / "main"
        if (scalaVersion.value.startsWith("2.")) Seq(crossSourceDirectory / "scala-2")
        else Nil
      },
      Test / scalacOptions ++= (if (scalaVersion.value.startsWith("2.")) Nil else Seq("-Yretain-trees"))
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
    .settings(
      `scala 2.12 to dotty`,
      publishSettings,
      name := "json-schema-circe",
      libraryDependencies += ("io.circe" %%% "circe-core" % circeVersion).cross(CrossVersion.for3Use2_13),
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
    .settings(
      `scala 2.12 to dotty`,
      publishSettings,
      name := "json-schema-playjson",
      libraryDependencies += ("com.typesafe.play" %%% "play-json" % playjsonVersion).cross(CrossVersion.for3Use2_13),
      (Compile / boilerplateSource) := baseDirectory.value / ".." / "src" / "main" / "boilerplate"
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))
    .dependsOnLocalCrossProjectsWithScope(
      "json-schema" -> "test->test;compile->compile"
    )

lazy val `json-schema-playjson-js` = `json-schema-playjson`.js
lazy val `json-schema-playjson-jvm` = `json-schema-playjson`.jvm
