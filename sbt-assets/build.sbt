name := "sbt-assets"

organization := "org.julienrf"

libraryDependencies += "commons-codec" % "commons-codec" % "1.10"

sbtPlugin := true

pomExtra :=
  <developers>
    <developer>
      <id>julienrf</id>
      <name>Julien Richard-Foy</name>
      <url>http://julien.richard-foy.fr</url>
    </developer>
  </developers>

scalacOptions in (Compile, doc) ++= Seq(
  "-doc-source-url",
  s"https://github.com/julienrf/endpoints/tree/v${version.value}€{FILE_PATH}.scala",
  "-sourcepath",
  baseDirectory.in(LocalRootProject).value.getAbsolutePath
)

autoAPIMappings := true

homepage := Some(url(s"https://github.com/julienrf/endpoints"))

licenses := Seq(
  "MIT License" -> url("http://opensource.org/licenses/mit-license.php")
)

scmInfo := Some(
  ScmInfo(
    url(s"https://github.com/julienrf/endpoints"),
    s"scm:git:git@github.com:julienrf/endpoints.git"
  )
)
