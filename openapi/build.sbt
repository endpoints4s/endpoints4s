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
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("openapi"))
    .settings(
      publishSettings,
      `scala 2.12 to dotty`,
      name := "openapi",
      // versionPolicyIntention := Compatibility.None,
      (Compile / boilerplateSource) := (Compile / baseDirectory).value / ".." / "src" / "main" / "boilerplate",
      libraryDependencies += "com.lihaoyi" %%% "ujson" % ujsonVersion,
    )
    .enablePlugins(spray.boilerplate.BoilerplatePlugin)
    .dependsOnLocalCrossProjectsWithNative("algebra", "json-schema")
    .dependsOnLocalCrossProjectsWithScopeWithNative(
      "algebra-testkit" -> Test,
      "json-schema-testkit" -> Test
    )
    .dependsOnLocalCrossProjectsWithScopeWithNative("json-schema-generic" -> Test)
    .configurePlatforms(JSPlatform, NativePlatform)(_.disablePlugins(ScoverageSbtPlugin))
    .nativeSettings(
      // `UUID.randomUUID()` needs `SecureRandom`, which this provides on top of openssl
      libraryDependencies += "com.github.lolgab" %%% "scala-native-crypto" % "0.4.0" % Test,
    )

lazy val `openapi-js` = openapi.js
lazy val `openapi-jvm` = openapi.jvm
lazy val `openapi-native` = openapi.native
