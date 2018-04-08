
import algebras.AlgebraCirceModule
import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalajslib._
import ammonite.ops.up

val `scala 2.10 to 2.12` = Seq("2.10.7", "2.11.12", "2.12.4")

val scalaTest = ivy"org.scalatest::scalatest::3.0.1"

trait EndpointsModule extends SbtModule with PublishModule {
  def crossVersion: String

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
      Developer("julienrf", "Julien Richard-Foy", "http://julien.richard-foy.fr")
    )
  )

  val circeVersion = "0.9.0"

  override def millSourcePath = super.millSourcePath / up

  trait EndpointsTests extends Tests {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.0.4")

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

}


//  trait EndpointsSJSModule extends EndpointsModule with ScalaJSModule {
//    override def scalaJSVersion = "0.6.22"
//  }

object algebras extends Module {

  object algebra extends mill.Cross[AlgebraModule](`scala 2.10 to 2.12`: _*)

  object `algebra-circe` extends mill.Cross[AlgebraCirceModule](`scala 2.10 to 2.12`: _*)

  class AlgebraModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-algebra"

    override def moduleDeps = Seq(openapi.jsonSchema(crossVersion))
  }

  class AlgebraCirceModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-algebra-circe"

    override def ivyDeps = Agg(
      ivy"io.circe::circe-core::$circeVersion"
    )

    override def moduleDeps = Seq(algebra(crossVersion))
  }

}

object openapi extends Module {

  object jsonSchema extends mill.Cross[JsonSchemaModule](`scala 2.10 to 2.12`: _*)

  object openapi extends mill.Cross[OpenApiModule](`scala 2.10 to 2.12`: _*)

  class JsonSchemaModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-openapi-json-schema"

    override def millSourcePath = super.millSourcePath / up / "json-schema"

    object test extends EndpointsTests
  }

  class OpenApiModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-openapi"

    override def moduleDeps = Seq(jsonSchema(crossVersion), algebras.algebra(crossVersion))

    override def ivyDeps = Agg(
      ivy"io.circe::circe-core::$circeVersion"
    )

    object test extends EndpointsTests {
      override def moduleDeps = super.moduleDeps ++ Seq(jsonSchema(crossVersion).test)
    }

  }

}



