import $file.common
import common.{EndpointsModule, EndpointsJvmModule, `scala 2.11 to 2.12`}

import mill.scalalib._
import mill._
import ammonite.ops.up

trait AkkaHttpModule extends Module {

  val akkaHttpVersion = "10.0.10"

  //dependencies
  def algebraJvm(crossVersion: String): EndpointsJvmModule
  def algebraCirceJvm(crossVersion: String): EndpointsJvmModule

  override def millSourcePath = super.millSourcePath / up / "akka-http"

  object client extends mill.Cross[AkkaHttpClientModule](`scala 2.11 to 2.12`: _*)

  object server extends mill.Cross[AkkaHttpClientModule](`scala 2.11 to 2.12`: _*)


  class AkkaHttpClientModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-akkahttp-client"

    override def moduleDeps = Seq(algebraJvm(crossVersion))

    override def ivyDeps = Agg(
      ivy"com.typesafe.akka::akka-http:$akkaHttpVersion"
    )

    object test extends Tests with EndpointsTests {
      override def ivyDeps = Agg(
        ivy"com.typesafe.akka::akka-http-testkit:$akkaHttpVersion"
      )
      override def moduleDeps = super.moduleDeps ++ Seq(
        algebraJvm(crossVersion).test,
        algebraCirceJvm(crossVersion).test
      )
    }

  }

  class AkkaHttpServerModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-akkahttp-server"

    override def moduleDeps = Seq(algebraJvm(crossVersion))

    override def ivyDeps = Agg(
      ivy"com.typesafe.akka::akka-http:$akkaHttpVersion"
    )

    object test extends Tests with EndpointsTests {
      override def ivyDeps = Agg(
        ivy"com.typesafe.akka::akka-http-testkit:$akkaHttpVersion"
      )
      override def moduleDeps = super.moduleDeps ++ Seq(
        algebraJvm(crossVersion).test,
        algebraCirceJvm(crossVersion).test
      )
    }

  }


}