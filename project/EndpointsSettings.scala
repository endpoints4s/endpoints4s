import sbt._
import sbt.Keys._
import com.typesafe.sbt.pgp.PgpKeys.{pgpPassphrase, pgpPublicRing, pgpSecretRing}
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
      "-encoding", "UTF-8",
      "-unchecked",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xfuture"
    ) ++
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 13 => Seq("-Xsource:2.14")
      case _ => Seq("-Yno-adapted-args", "-Ywarn-unused-import", "-Xexperimental")
    })
  )
  val `scala 2.11` = Seq(
    scalaVersion := "2.11.12",
    crossScalaVersions := Seq("2.11.12")
  )
  val `scala 2.11 to 2.12` = Seq(
    scalaVersion := "2.12.8",
    crossScalaVersions := Seq("2.11.12", "2.12.8")
  )
  val `scala 2.11 to latest` = Seq(
    scalaVersion := "2.12.8",
    crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.0-M5")
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
      "-doc-source-url", s"https://github.com/julienrf/endpoints/tree/v${version.value}€{FILE_PATH}.scala",
      "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath
    ),
    apiURL := Some(url(s"http://julienrf.github.io/endpoints/api/${version.value}/")),
    autoAPIMappings := true,
    homepage := Some(url(s"https://github.com/julienrf/endpoints")),
    licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php")),
    scmInfo := Some(
      ScmInfo(
        url(s"https://github.com/julienrf/endpoints"),
        s"scm:git:git@github.com:julienrf/endpoints.git"
      )
    )
  )

  val noPublishSettings = commonSettings ++ Seq(
    publishArtifact := false,
    publish := ()
    //  publishLocal := ()
  )

  // --- Common dependencies

  val circeVersion = "0.11.1"
  val playjsonVersion = "2.6.13"
  val playVersion = "2.6.21"
  val sttpVersion = "1.5.6"

  val scalaTestVersion = "3.0.8-RC1"
  val scalaTestDependency = "org.scalatest" %% "scalatest" % scalaTestVersion % Test
  val addScalaTestCrossDependency = libraryDependencies += "org.scalatest" %%% "scalatest" % scalaTestVersion % Test

}
