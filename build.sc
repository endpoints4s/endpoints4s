
import algebras.AlgebraCirceModule
import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalajslib._
import ammonite.ops.up
import mill.define.{Discover, ExternalModule}
import mill.eval.Evaluator

import scala.xml.XML

//val `scala 2.10 to 2.12` = Seq("2.10.7", "2.11.12", "2.12.4")
val `scala 2.10 to 2.12` = Seq("2.12.4")

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
  val playVersion = "2.6.7"

  override def millSourcePath = super.millSourcePath / up

  trait EndpointsTests extends TestModule {
    override def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.0.4")

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

}


//  trait EndpointsSJSModule extends EndpointsModule with ScalaJSModule {
//    override def scalaJSVersion = "0.6.22"
//  }

object algebras extends Module {

  object algebra extends mill.Cross[AlgebraModule](`scala 2.10 to 2.12`: _*)

  object algebraCirce extends mill.Cross[AlgebraCirceModule](`scala 2.10 to 2.12`: _*)

  object algebraPlayjson extends mill.Cross[AlgebraPlayjsonModule](`scala 2.10 to 2.12`: _*)

  class AlgebraModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-algebra"

    override def moduleDeps = Seq(openapi.jsonSchema(crossVersion))

    object test extends Tests with EndpointsTests {
      override def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"com.github.tomakehurst:wiremock:2.6.0"
      )
    }

  }

  class AlgebraCirceModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-algebra-circe"

    override def millSourcePath = super.millSourcePath / up / "algebra-circe"

    override def ivyDeps = Agg(
      ivy"io.circe::circe-parser::$circeVersion"
    )

    override def moduleDeps = Seq(algebra(crossVersion))

    object test extends Tests with EndpointsTests {
      override def moduleDeps = super.moduleDeps ++ Seq(algebras.algebra(crossVersion).test)

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
      override def moduleDeps = super.moduleDeps ++ Seq(algebras.algebra(crossVersion).test)
    }

  }

}

object openapi extends Module {

  object jsonSchema extends mill.Cross[JsonSchemaModule](`scala 2.10 to 2.12`: _*)

  object openapi extends mill.Cross[OpenApiModule](`scala 2.10 to 2.12`: _*)

  class JsonSchemaModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-openapi-json-schema"

    override def millSourcePath = super.millSourcePath / up / "json-schema"

    object test extends Tests with EndpointsTests

  }

  class OpenApiModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-openapi"

    override def moduleDeps = Seq(jsonSchema(crossVersion), algebras.algebra(crossVersion))

    override def ivyDeps = Agg(
      ivy"io.circe::circe-core::$circeVersion"
    )

    object test extends Tests with EndpointsTests {
      override def moduleDeps = super.moduleDeps ++ Seq(jsonSchema(crossVersion).test)
    }

  }

}

object akkaHttp extends Module {

  val akkaHttpVersion = "10.0.10"

  override def millSourcePath = super.millSourcePath / up / "akka-http"

  object client extends mill.Cross[AkkaHttpClientModule](`scala 2.10 to 2.12`: _*)

  class AkkaHttpClientModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-akkahttp-client"

    override def moduleDeps = Seq(algebras.algebra(crossVersion))

    override def ivyDeps = Agg(
      ivy"com.typesafe.akka::akka-http:$akkaHttpVersion"
    )

    object test extends Tests with EndpointsTests {
      override def ivyDeps = Agg(
        ivy"com.typesafe.akka::akka-http-testkit:$akkaHttpVersion"
      )
      override def moduleDeps = super.moduleDeps ++ Seq(algebras.algebra(crossVersion).test)
    }

  }


}

def genidea(ev: Evaluator[Any]) = T.command {
  mill.scalalib.GenIdeaImpl(
    implicitly,
    ev.rootModule,
    ev.rootModule.millDiscover
  )
  import ammonite.ops._
  val files = (ls ! pwd / ".idea_modules")
    .filter(_.toString().endsWith("test.iml"))
  files.foreach { file =>
    val content = read(file)
    val regex = """<content url="file://\$MODULE_DIR\$/\.\./(.*)">""".r
    val replacement = """<content url="file://\$MODULE_DIR\$/\.\./$1/src/test">"""
    val newContent = regex.replaceFirstIn(content, replacement)
    write.over(file, newContent)
  }
}
