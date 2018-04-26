import $file.common
import common.{EndpointsJsModule, EndpointsModule, `scala 2.11 to 2.12`}

import mill._
import mill.scalalib._
import ammonite.ops.up
import mill.scalajslib.ScalaJSModule

trait XhrModule extends Module {

  def algebraJs(crossVersion: String): EndpointsJsModule
  def algebraCirceJs(crossVersion: String): EndpointsJsModule

  object client extends mill.Cross[XhrClientModule](`scala 2.11 to 2.12`: _*)

  object clientCirce extends mill.Cross[XhrClientCirceModule](`scala 2.11 to 2.12`: _*)

  object clientFaithful extends mill.Cross[XhrFaithfulCirceModule](`scala 2.11 to 2.12`: _*)

  class XhrClientModule(val crossVersion: String) extends EndpointsJsModule {
    override def artifactName = s"endpoints-xhr-client"

    override def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.scala-js::scalajs-dom::0.9.5"
    )

    override def moduleDeps = Seq(algebraJs(crossVersion))

    object test extends EndpointsJsTests {
      override def moduleDeps = super.moduleDeps ++ Seq(algebraJs(crossVersion).test)
    }

  }

  class XhrClientCirceModule(val crossVersion: String) extends EndpointsJsModule {
    override def artifactName = s"endpoints-xhr-client-circe"
    override def millSourcePath = super.millSourcePath / up / "client-circe"

    def ivyDeps = super.ivyDeps() ++ Agg(ivy"io.circe::circe-parser::$circeVersion")

    override def moduleDeps = Seq(algebraCirceJs(crossVersion), client(crossVersion))

    object test extends EndpointsJsTests

  }

  class XhrFaithfulCirceModule(val crossVersion: String) extends EndpointsJsModule {
    override def artifactName = s"endpoints-xhr-client-faithful"
    override def millSourcePath = super.millSourcePath / up / "client-faithful"

    def ivyDeps = super.ivyDeps() ++ Agg(ivy"org.julienrf::faithful::1.0.0")

    override def moduleDeps = Seq(client(crossVersion))

    object test extends EndpointsJsTests

  }


}