import $file.common
import common.{EndpointsModule, EndpointsJvmModule, `scala 2.10 to 2.12`}
import mill._
import mill.scalalib._
import ammonite.ops.up
import mill.scalajslib.{ScalaJSModule, _}


trait OpenapiModule extends Module {

  def algebra(crossVersion: String): EndpointsJvmModule

  object openapi extends mill.Cross[OpenApiModule](`scala 2.10 to 2.12`: _*)

  class OpenApiModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-openapi"

    override def moduleDeps = Seq(algebra(crossVersion))

    override def ivyDeps = Agg(
      ivy"io.circe::circe-core::$circeVersion"
    )

    object test extends Tests with EndpointsTests {
      override def moduleDeps = super.moduleDeps ++ Seq(algebra(crossVersion).test)
    }

  }

}