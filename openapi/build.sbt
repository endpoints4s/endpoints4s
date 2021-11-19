import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}
import EndpointsSettings._
import LocalCrossProject._
import com.typesafe.tools.mima.core.{
  DirectMissingMethodProblem,
  IncompatibleMethTypeProblem,
  ReversedMissingMethodProblem,
  ProblemFilters
}

lazy val openapi =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("openapi"))
    .settings(
      version := "3.1.0+n",
      versionPolicyIntention := Compatibility.None,
      publishSettings,
      `scala 2.12 to dotty`,
      name := "openapi",
      (Compile / boilerplateSource) := (Compile / baseDirectory).value / ".." / "src" / "main" / "boilerplate",
      libraryDependencies += ("com.lihaoyi" %%% "ujson" % ujsonVersion).cross(CrossVersion.for3Use2_13),
      excludeDependencies ++= {
        if (scalaBinaryVersion.value.startsWith("3")) {
          List(ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"))
        } else Nil
      }
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .dependsOnLocalCrossProjectsWithScope(
      "algebra" -> "test->test;compile->compile",
      "json-schema" -> "test->test;compile->compile",
      "json-schema-generic" -> "test->test"
    )
    .jsConfigure(_.disablePlugins(ScoverageSbtPlugin))

lazy val `openapi-js` = openapi.js
lazy val `openapi-jvm` = openapi.jvm
