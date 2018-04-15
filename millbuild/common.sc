import mill._
import mill.scalalib._
import mill.scalalib.publish._
import ammonite.ops.up


//val `scala 2.10 to 2.12` = Seq("2.10.7", "2.11.12", "2.12.4")
val `scala 2.10 to 2.12` = Seq("2.12.4")

trait EndpointsModule extends SbtModule with PublishModule {
  def crossVersion: String

  def scalaVersion = crossVersion

  def publishVersion = "0.0.2"

  def pomSettings = PomSettings(
    description = "description",
    organization = "org.julienrf",
    url = "http://julienrf.github.io/endpoints/",
    licenses = Seq(
      License("MIT license", "http://www.opensource.org/licenses/mit-license.php")
    ),
    scm = SCM(
      "git://github.com:julienrf/endpoints.git",
      "scm:git://github.com:julienrf/endpoints.git"
    ),
    developers = Seq(
      Developer("julienrf", "Julien Richard-Foy", "http://julien.richard-foy.fr")
    )
  )

  val circeVersion = "0.9.0"
  val playVersion = "2.6.7"

  override def millSourcePath = super.millSourcePath / up

  trait EndpointsTests extends TestModule {
    override def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.0.4")

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

  def test: EndpointsTests
}

//  trait EndpointsSJSModule extends EndpointsModule with ScalaJSModule {
//    override def scalaJSVersion = "0.6.22"
//  }
