import $file.common
import common.{EndpointsJsModule, EndpointsModule, `scala 2.10 to 2.12`}

import mill._
import mill.scalalib._
import ammonite.ops.up
import mill.scalajslib.ScalaJSModule

trait XhrModule extends Module {

  def algebraJs(crossVersion: String): EndpointsJsModule

  object client extends mill.Cross[XhrClientModule](`scala 2.10 to 2.12`: _*)

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

}