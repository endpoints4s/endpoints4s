
import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalajslib._
import ammonite.ops.up

object algebras extends mill.Cross[AlgebrasModule]("2.10.7", "2.11.12", "2.12.4")

class AlgebrasModule(crossVersion: String) extends Module {

  override def millSourcePath = super.millSourcePath /  up / up / "algebras"

  val circeVersion = "0.9.0"

  trait EndpointsModule extends SbtModule with PublishModule {
    def scalaVersion = crossVersion

    def publishVersion = "0.0.1"
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
        Developer("julienrf", "Julien Richard-Foy","http://julien.richard-foy.fr")
      )
    )


  }

  trait EndpointsSJSModule extends EndpointsModule with ScalaJSModule {
    override def scalaJSVersion = "0.6.22"
  }

  object algebra extends EndpointsSJSModule {
    override def artifactName = s"endpoints-algebra"
  }

  object `algebra-circe` extends EndpointsSJSModule {
    override def artifactName = s"endpoints-algebra-circe"
    override def ivyDeps = Agg(
      ivy"io.circe::circe-core::$circeVersion"
    )
    override def moduleDeps = Seq(algebra)
  }
}
