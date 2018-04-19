import $file.common
import common.{EndpointsModule, EndpointsJsModule, `scala 2.10 to 2.12`}

import mill._
import mill.scalalib._
import ammonite.ops._


trait AlgebrasModule  extends Module {

  def jsonSchema(crossVersion: String): EndpointsModule
  def jsonSchemaJs(crossVersion: String): EndpointsJsModule

  object algebra extends mill.Cross[AlgebraModule](`scala 2.10 to 2.12`: _*)
  object algebraJs extends mill.Cross[AlgebraJsModule](`scala 2.10 to 2.12`: _*)

  object algebraCirce extends mill.Cross[AlgebraCirceModule](`scala 2.10 to 2.12`: _*)

  object algebraPlayjson extends mill.Cross[AlgebraPlayjsonModule](`scala 2.10 to 2.12`: _*)

  class AlgebraModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-algebra"

    override def moduleDeps = Seq(jsonSchema(crossVersion))

    object test extends Tests with EndpointsTests {
      override def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"com.github.tomakehurst:wiremock:2.6.0"
      )
    }

  }
  class AlgebraJsModule(crossVersion: String) extends AlgebraModule(crossVersion) with EndpointsJsModule {
    override def millSourcePath = super.millSourcePath / up / "algebra"

    override def moduleDeps = Seq(jsonSchemaJs(crossVersion))
  }

  class AlgebraCirceModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-algebra-circe"

    override def millSourcePath = super.millSourcePath / up / "algebra-circe"

    override def ivyDeps = Agg(
      ivy"io.circe::circe-parser::$circeVersion"
    )

    override def moduleDeps = Seq(algebra(crossVersion))

    object test extends Tests with EndpointsTests {
      override def moduleDeps = super.moduleDeps ++ Seq(algebra(crossVersion).test)

      override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(
        ivy"org.scalamacros:::paradise:2.1.0"
      )

      override def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"io.circe::circe-generic:$circeVersion"
      )
    }

  }

  class AlgebraPlayjsonModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-algebra-playjson"

    override def millSourcePath = super.millSourcePath / up / "algebra-playjson"

    override def ivyDeps = Agg(ivy"com.typesafe.play::play-json:$playVersion")

    override def moduleDeps = Seq(algebra(crossVersion))

    object test extends Tests with EndpointsTests {
      override def moduleDeps = super.moduleDeps ++ Seq(algebra(crossVersion).test)
    }

  }

}
