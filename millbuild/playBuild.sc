import $file.common
import common.{EndpointsModule, EndpointsJvmModule, `scala 2.11 to 2.12`}
import mill._
import mill.scalalib._
import ammonite.ops.up

trait PlayModule extends Module {

  val playVersion = "2.6.7"

  def algebraJvm(crossVersion: String): EndpointsJvmModule
  def algebraCirceJvm(crossVersion: String): EndpointsJvmModule

  object client extends mill.Cross[PlayClientModule](`scala 2.11 to 2.12`: _*)

  object server extends mill.Cross[PlayServerModule](`scala 2.11 to 2.12`: _*)

  object serverCirce extends mill.Cross[PlayServerCirceModule](`scala 2.11 to 2.12`: _*)

  class PlayClientModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-play-client"

    override def moduleDeps = Seq(algebraJvm(crossVersion))

    override def ivyDeps = Agg(
      ivy"com.typesafe.play::play-ahc-ws:$playVersion"
    )

    object test extends Tests with EndpointsTests {
      override def moduleDeps = super.moduleDeps ++ Seq(algebraJvm(crossVersion).test)
    }
  }

  class PlayServerModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-play-server"

    override def moduleDeps = Seq(algebraJvm(crossVersion))

    override def ivyDeps = Agg(
      ivy"com.typesafe.play::play-netty-server:$playVersion"
    )

    object test extends Tests with EndpointsTests {
      override def moduleDeps = super.moduleDeps ++ Seq(algebraJvm(crossVersion).test)
    }
  }

  class PlayServerCirceModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-play-server-circe"
    override def millSourcePath = super.millSourcePath / up / "server-circe"

    override def moduleDeps = Seq(server(crossVersion), algebraCirceJvm(crossVersion))

    override def ivyDeps = Agg(
      ivy"io.circe::circe-parser:$circeVersion"
    )
  }

}