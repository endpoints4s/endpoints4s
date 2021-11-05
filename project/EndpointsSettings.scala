import sbt._
import sbt.Keys._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport.mimaPreviousArtifacts

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
    scalaVersion := "2.13.6",
    crossScalaVersions := Seq("2.13.6")
  )
  val `scala 2.12 to 2.13` = Seq(
    scalaVersion := "2.13.6",
    crossScalaVersions := Seq("2.13.6", "2.12.13")
  )
  val `scala 2.12 to dotty` = Seq(
    scalaVersion := "2.13.6",
    crossScalaVersions := Seq("2.13.6", "3.0.2", "2.12.13")
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
    homepage := Some(url(s"https://github.com/endpoints4s/endpoints4s")),
    licenses := Seq(
      "MIT License" -> url("http://opensource.org/licenses/mit-license.php")
    )
  )

  val noPublishSettings = commonSettings ++ Seq(
    publishArtifact := false,
    publish := { () },
    publishLocal := { () },
    mimaPreviousArtifacts := Set.empty
  )

  // --- Common dependencies

  val circeVersion = "0.14.1"
  val playjsonVersion = "2.9.2"
  val playVersion = "2.8.7"
  val sttpVersion = "3.3.16"
  val akkaActorVersion = "2.6.15"
  val akkaHttpVersion = "10.2.6"
  val http4sVersion = "0.23.1"
  val ujsonVersion = "1.4.0"

  val scalaTestVersion = "3.2.10"
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
        case Some((2, n)) if n >= 13 => Nil
        case _ =>
          compilerPlugin(
            "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
          ) :: Nil
      }
    }
  )
}
