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
      version := "3.0.0+n",
      publishSettings,
      `scala 2.12 to dotty`,
      name := "openapi",
      (Compile / boilerplateSource) := (Compile / baseDirectory).value / ".." / "src" / "main" / "boilerplate",
      libraryDependencies += ("com.lihaoyi" %%% "ujson" % ujsonVersion).cross(CrossVersion.for3Use2_13),
      excludeDependencies ++= {
        if (scalaBinaryVersion.value.startsWith("3")) {
          List(ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"))
        } else Nil
      },
      mimaBinaryIssueFilters ++= Seq(
        ProblemFilters.exclude[IncompatibleMethTypeProblem]("endpoints4s.openapi.model.OpenApi.this"),

        // Due to adding a new parameter to default `private` constructors
        // OK since the constructors are `private`
        ProblemFilters.exclude[DirectMissingMethodProblem]("endpoints4s.openapi.model.Schema#AllOf.this"),
        ProblemFilters.exclude[DirectMissingMethodProblem]("endpoints4s.openapi.model.Schema#Array.this"),
        ProblemFilters.exclude[DirectMissingMethodProblem]("endpoints4s.openapi.model.Schema#Enum.this"),
        ProblemFilters.exclude[DirectMissingMethodProblem]("endpoints4s.openapi.model.Schema#Object.this"),
        ProblemFilters.exclude[DirectMissingMethodProblem]("endpoints4s.openapi.model.Schema#OneOf.this"),
        ProblemFilters.exclude[DirectMissingMethodProblem]("endpoints4s.openapi.model.Schema#Primitive.this"),
        ProblemFilters.exclude[DirectMissingMethodProblem]("endpoints4s.openapi.model.Schema#Reference.this"),

        // Due to adding a new member to a Scala `sealed` class
        // OK since `Schema` is sealed and subtypes are final
        ProblemFilters.exclude[ReversedMissingMethodProblem]("endpoints4s.openapi.model.Schema.default")
      )
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
