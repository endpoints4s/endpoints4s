import sbt._
import sbt.Keys._
import com.jsuereth.sbtpgp.PgpKeys.{pgpPassphrase, pgpPublicRing, pgpSecretRing}
import xerial.sbt.Sonatype.autoImport.sonatypePublishTo

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType, _}
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._

object EndpointsSettings {

  val commonSettings = Seq(
    organization := "org.julienrf",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-unchecked",
      "-language:implicitConversions",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard"
    ) ++
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 =>
          Seq(
            "-Xlint:adapted-args,nullary-unit,inaccessible,nullary-override,infer-any,missing-interpolator,doc-detached,private-shadow,type-parameter-shadow,poly-implicit-overload,option-implicit,delayedinit-select,package-object-classes,stars-align,constant,unused,nonlocal-return,implicit-not-found,serial,valpattern,eta-zero,eta-sam,deprecation"
          ) ++ (if (insideCI.value) Seq("-Xfatal-warnings") else Nil)
        case _ =>
          Seq(
            "-Yno-adapted-args",
            "-Ywarn-unused-import",
            "-Xexperimental",
            "-Xfuture",
            "-language:higherKinds"
          )
      }),
    // Remove scala-compiler dependency automatically added by the sbt-heroku plugin
    libraryDependencies -= "org.scala-lang" % "scala-compiler" % scalaVersion.value % Runtime
  )
  val `scala 2.13` = Seq(
    scalaVersion := "2.13.2",
    crossScalaVersions := Seq("2.13.2")
  )
  val `scala 2.12 to latest` = Seq(
    scalaVersion := "2.13.2",
    crossScalaVersions := Seq("2.13.2", "2.12.11")
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
    scalacOptions in (Compile, doc) ++= Seq(
      "-doc-source-url",
      s"https://github.com/julienrf/endpoints/tree/v${version.value}€{FILE_PATH}.scala",
      "-sourcepath",
      baseDirectory.in(LocalRootProject).value.getAbsolutePath
    ),
    apiURL := Some(
      url(s"http://julienrf.github.io/endpoints/api/${version.value}/")
    ),
    autoAPIMappings := true,
    homepage := Some(url(s"https://github.com/julienrf/endpoints")),
    licenses := Seq(
      "MIT License" -> url("http://opensource.org/licenses/mit-license.php")
    ),
    scmInfo := Some(
      ScmInfo(
        url(s"https://github.com/julienrf/endpoints"),
        s"scm:git:git@github.com:julienrf/endpoints.git"
      )
    )
  )

  val noPublishSettings = commonSettings ++ Seq(
    publishArtifact := false,
    publish := { () },
    publishLocal := { () }
  )

  // --- Common dependencies

  val circeVersion = "0.13.0"
  val playjsonVersion = "2.8.1"
  val playVersion = "2.8.1"
  val sttpVersion = "1.7.2"
  val akkaActorVersion = "2.6.3"
  val akkaHttpVersion = "10.1.12"
  val http4sVersion = "0.21.4"
  val ujsonVersion = "1.0.0"

  val scalaTestVersion = "3.1.1"
  val scalaTestDependency =
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test
  val addScalaTestCrossDependency =
    libraryDependencies += "org.scalatest" %%% "scalatest" % scalaTestVersion % Test
  val macroParadiseDependency = Seq(
    scalacOptions in Compile ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => "-Ymacro-annotations" :: Nil
        case _                       => Nil
      }
    },
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => Nil
        case _ =>
          compilerPlugin(
            "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
          ) :: Nil
      }
    }
  )
}
