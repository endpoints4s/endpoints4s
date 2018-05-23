import $file.common
import common.{EndpointsModule, EndpointsJvmModule, `scala 2.11 to 2.12`}
import mill._
import mill.scalalib._
import ammonite.ops.up

trait ScalajModule extends Module {

  def algebra(crossVersion: String): EndpointsJvmModule

  object client extends mill.Cross[ScalajClientModule](`scala 2.11 to 2.12`: _*)

  class ScalajClientModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-scalaj-client"

    override def moduleDeps = Seq(algebra(crossVersion))

    override def ivyDeps = Agg(
      ivy"org.scalaj::scalaj-http:2.3.0"
    )

    object test extends Tests with EndpointsTests {
      override def moduleDeps = super.moduleDeps ++ Seq(algebra(crossVersion).test)
    }
  }

}