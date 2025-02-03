import sbt._
import sbt.Keys._
import sbtversionpolicy.SbtVersionPolicyPlugin.autoImport.versionPolicyIntention
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._

object EndpointsSettings {

  val commonSettings = Seq(
    organization := "org.endpoints4s",
    // Scala 2.x vs 3.x
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, _)) =>
          Seq(
            "-feature",
            "-deprecation",
            "-encoding",
            "UTF-8",
            "-unchecked",
            "-language:implicitConversions",
            "-Ywarn-dead-code",
            "-Ywarn-numeric-widen",
            "-Ywarn-value-discard"
          )
        case _ =>
          Seq(
            "-feature",
            "-deprecation",
            "-encoding",
            "UTF-8",
            "-unchecked",
            "-language:implicitConversions,Scala2Compat"
          )
      }
    },
    // Scala 2.12 vs 2.13
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 =>
          Seq(
            "-Xlint:adapted-args,nullary-unit,inaccessible,infer-any,missing-interpolator,doc-detached,private-shadow,type-parameter-shadow,poly-implicit-overload,option-implicit,delayedinit-select,package-object-classes,stars-align,constant,unused,nonlocal-return,implicit-not-found,serial,valpattern,eta-zero,eta-sam,deprecation"
          ) ++ (if (insideCI.value) Seq("-Xfatal-warnings") else Nil)
        case Some((2, _)) =>
          Seq(
            "-Xlint",
            "-Yno-adapted-args",
            "-Ywarn-unused-import",
            "-Xexperimental",
            "-Xfuture",
            "-language:higherKinds"
          )
        case _ =>
          Seq()
      }
    },
    // Remove scala-compiler dependency automatically added by the sbt-heroku plugin
    libraryDependencies -= "org.scala-lang" % "scala-compiler" % scalaVersion.value % Runtime
  )
  val `scala 2.13` = Seq(
    scalaVersion := "2.13.14",
    crossScalaVersions := Seq("2.13.14")
  )
  val `scala 2.12 to 2.13` = Seq(
    scalaVersion := "2.13.14",
    crossScalaVersions := Seq("2.13.14", "2.12.19")
  )
  val `scala 2.12 to dotty` = Seq(
    scalaVersion := "2.13.14",
    crossScalaVersions := Seq("2.13.14", "3.3.5", "2.12.19")
  )

  val publishSettings = commonSettings ++ Seq(
    pomExtra :=
      <developers>
        <developer>
          <id>julienrf</id>
          <name>Julien Richard-Foy</name>
          <url>http://julien.richard-foy.fr</url>
        </developer>
      </developers>,
    apiURL := Some(
      url(s"http://endpoints4s.github.io/api")
    ),
    autoAPIMappings := true,
    Compile / doc / scalacOptions += "-no-link-warnings",
    homepage := Some(url(s"https://github.com/endpoints4s/endpoints4s")),
    licenses := Seq(
      "MIT License" -> url("http://opensource.org/licenses/mit-license.php")
    ),
    version := Versioning.computeVersion(
      name.value,
      crossVersion.value,
      scalaBinaryVersion.value,
      scalaVersion.value,
      versionPolicyIntention.value
    )
  )

  val noPublishSettings = commonSettings ++ Seq(
    publishArtifact := false,
    publish := { () },
    publishLocal := { () },
    publish / skip := true
  )

  // --- Common dependencies

  val circeVersion = "0.14.1"
  val playjsonVersion = "2.9.3"
  val playVersion = "2.8.13"
  val sttpVersion = "3.3.18"
  val pekkoActorVersion = "1.0.3"
  val pekkoHttpVersion = "1.0.1"
  val http4sVersion = "0.23.6"
  val http4sDomVersion = "0.2.3"
  val ujsonVersion = "3.3.1"

  val scalaTestVersion = "3.2.17"
  val scalaTestDependency =
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test
  val macroParadiseDependency = Seq(
    Compile / scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => "-Ymacro-annotations" :: Nil
        case _                       => Nil
      }
    },
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n <= 12 =>
          compilerPlugin(
            "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
          ) :: Nil
        case _ => Nil
      }
    }
  )
}
