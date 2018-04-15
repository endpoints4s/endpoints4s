import $file.millbuild.common
import $file.millbuild.akkahttp
import $file.millbuild.algebrasBuild

import common.{EndpointsModule, `scala 2.10 to 2.12`}
import akkahttp.AkkaHttpModule
import algebrasBuild.AlgebrasModule
import mill._
import mill.scalalib._
import mill.scalajslib._
import ammonite.ops.up
import mill.eval.Evaluator

object algebras extends AlgebrasModule {
  override def jsonSchema(crossVersion: String) =  openapi.jsonSchema(crossVersion)
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

object akkaHttp extends AkkaHttpModule {

  override def algebra(crossVersion: String) = algebras.algebra(crossVersion).asInstanceOf[EndpointsModule]

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
