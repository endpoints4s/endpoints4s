import EndpointsSettings._

val `xhr-client` =
  project.in(file("client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(publishSettings ++ `scala 2.11 to 2.12`)
    .settings(
      name := "endpoints-xhr-client",
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"
    )
    .dependsOn(LocalProject("algebraJS"))

val `xhr-client-faithful` =
  project.in(file("client-faithful"))
    .enablePlugins(ScalaJSPlugin)
    .settings(publishSettings ++ `scala 2.11 to 2.12`)
    .settings(
      name := "endpoints-xhr-client-faithful",
      libraryDependencies += "org.julienrf" %%% "faithful" % "1.0.0"
    )
    .dependsOn(`xhr-client`)

val `xhr-client-circe` =
  project.in(file("client-circe"))
    .enablePlugins(ScalaJSPlugin)
    .settings(publishSettings ++ `scala 2.11 to 2.12`)
    .settings(
      name := "endpoints-xhr-client-circe",
      libraryDependencies += "io.circe" %%% "circe-parser" % circeVersion
    )
    .dependsOn(`xhr-client`, LocalProject("algebra-circeJS"))
