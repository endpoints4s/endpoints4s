import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import EndpointsSettings._
import LocalCrossProject._

val `json-schema` =
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("json-schema"))
    .settings(
      `scala 2.12 to dotty`,
      publishSettings,
      name := "algebra-json-schema",
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %%% "scala-collection-compat" % "2.7.0",
        "org.scalacheck" %%% "scalacheck" % "1.17.0" % Test,
        scalaTestDependency
      ),
      (Compile / boilerplateSource) := baseDirectory.value / ".." / "src" / "main" / "boilerplate"
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .configurePlatforms(JSPlatform, NativePlatform)(_.disablePlugins(ScoverageSbtPlugin))

val `json-schema-js` = `json-schema`.js
val `json-schema-jvm` = `json-schema`.jvm
val `json-schema-native` = `json-schema`.native

val `json-schema-testkit` =
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("testkit"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "algebra-json-schema-testkit",
      libraryDependencies ++= Seq(
        "org.scalatest" %%% "scalatest" % scalaTestVersion,
        "io.github.cquiroz" %%% "scala-java-time" % "2.4.0",
        "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.4.0"
      )
    )
    .dependsOn(`json-schema`)
    .configurePlatforms(JSPlatform, NativePlatform)(_.disablePlugins(ScoverageSbtPlugin))

val `json-schema-testkit-js` = `json-schema-testkit`.js
val `json-schema-testkit-jvm` = `json-schema-testkit`.jvm
val `json-schema-testkit-native` = `json-schema-testkit`.native

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
          if (scalaVersion.value.startsWith("2.")) "com.chuusai" %%% "shapeless" % "2.3.13"
          else "org.typelevel" %%% "shapeless3-deriving" % "3.0.4"
        shapelessDependency +: commonDependencies
      },
      (Test / boilerplateSource) := baseDirectory.value / ".." / "src" / "test" / "boilerplate",
      Test / scalacOptions ++= (if (scalaVersion.value.startsWith("2.")) Nil
                                else Seq("-Yretain-trees"))
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))
    .dependsOnLocalCrossProjects("json-schema")
    .dependsOnLocalCrossProjectsWithScope(
      "json-schema-circe" -> Test
    )

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
      libraryDependencies += "io.circe" %%% "circe-core" % circeVersion,
      (Compile / boilerplateSource) := baseDirectory.value / ".." / "src" / "main" / "boilerplate"
    )
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .dependsOnLocalCrossProjects(
      "algebra-circe", // Needed only because of CirceCodec, but that class doesn’t depend on the algebra
      "json-schema"
    )
    .dependsOnLocalCrossProjectsWithScope(
      "json-schema-testkit" -> Test
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
      libraryDependencies += ("com.typesafe.play" %%% "play-json" % playjsonVersion).cross(
        CrossVersion.for3Use2_13
      ),
      (Compile / boilerplateSource) := baseDirectory.value / ".." / "src" / "main" / "boilerplate"
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))
    .dependsOnLocalCrossProjects("json-schema")
    .dependsOnLocalCrossProjectsWithScope(
      "json-schema-testkit" -> Test
    )

lazy val `json-schema-playjson-js` = `json-schema-playjson`.js
lazy val `json-schema-playjson-jvm` = `json-schema-playjson`.jvm
