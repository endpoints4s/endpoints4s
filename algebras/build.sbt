import EndpointsSettings._

val `json-schema` =
  crossProject.crossType(CrossType.Pure).in(file("json-schema"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-algebra-json-schema",
      addScalaTestCrossDependency
    )

val `json-schema-js` = `json-schema`.js
val `json-schema-jvm` = `json-schema`.jvm

val algebra =
  crossProject.crossType(CrossType.Pure).in(file("algebra"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-algebra",
      libraryDependencies ++= Seq(
        "com.github.tomakehurst" % "wiremock" % "2.6.0" % Test,
        "org.scalatest" %%% "scalatest" % scalaTestVersion % Test
      )
    )
    .dependsOn(`json-schema` % "test->test;compile->compile")

val `algebra-js` = algebra.js

val `algebra-jvm` = algebra.jvm

val `algebra-circe` =
  crossProject.crossType(CrossType.Pure).in(file("algebra-circe"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-algebra-circe",
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-parser" % circeVersion,
        "io.circe" %%% "circe-generic" % circeVersion % Test,
        compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" % Test cross CrossVersion.full)
      )
    )
    .dependsOn(`algebra` % "test->test;compile->compile")

val `algebra-circe-js` = `algebra-circe`.js

val `algebra-circe-jvm` = `algebra-circe`.jvm

val `algebra-playjson` =
  crossProject.crossType(CrossType.Pure).in(file("algebra-playjson"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-algebra-playjson",
      libraryDependencies += "com.typesafe.play" %%% "play-json" % playVersion
    )
    .dependsOn(`algebra` % "test->test;compile->compile")

val `algebra-playjson-js` = `algebra-playjson`.js
val `algebra-playjson-jvm` = `algebra-playjson`.jvm

lazy val `json-schema-circe` =
  crossProject.crossType(CrossType.Pure).in(file("json-schema-circe"))
    .settings(publishSettings ++ `scala 2.10 to 2.12`: _*)
    .settings(
      name := "endpoints-json-schema-circe",
      libraryDependencies += "io.circe" %%% "circe-core" % circeVersion
    )
    .jsConfigure(_.dependsOn(`json-schema-js` % "test->test;compile->compile"))
    .jvmConfigure(_.dependsOn(`json-schema-jvm` % "test->test;compile->compile"))
    .dependsOn(`algebra-circe`) // Needed only because of CirceCodec, but that class doesn’t depend on the algebra

lazy val `json-schema-circe-js` = `json-schema-circe`.js
lazy val `json-schema-circe-jvm` = `json-schema-circe`.jvm