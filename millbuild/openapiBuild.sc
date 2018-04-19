import $file.common
import common.{EndpointsModule, EndpointsJsModule, `scala 2.10 to 2.12`}
import mill._
import mill.scalalib._
import ammonite.ops.up
import mill.scalajslib.{ScalaJSModule, _}


trait OpenapiModule extends Module {

  def algebra(crossVersion: String): EndpointsModule

  //TODO move out of here? it should probably be part of algebra package
  object jsonSchema extends mill.Cross[JsonSchemaModule](`scala 2.10 to 2.12`: _*)

  object jsonSchemaJs extends mill.Cross[JsonSchemaJsModule](`scala 2.10 to 2.12`: _*)

  object openapi extends mill.Cross[OpenApiModule](`scala 2.10 to 2.12`: _*)

  class JsonSchemaModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-openapi-json-schema"

    override def millSourcePath = super.millSourcePath / up / "json-schema"

    object test extends Tests with EndpointsTests

  }

  class JsonSchemaJsModule(crossVersion: String) extends JsonSchemaModule(crossVersion) with EndpointsJsModule {

    override def artifactName = s"endpoints-openapi-json-schema"

  }

  class OpenApiModule(val crossVersion: String) extends EndpointsModule {
    override def artifactName = s"endpoints-openapi"

    override def moduleDeps = Seq(jsonSchema(crossVersion), algebra(crossVersion))

    override def ivyDeps = Agg(
      ivy"io.circe::circe-core::$circeVersion"
    )

    object test extends Tests with EndpointsTests {
      override def moduleDeps = super.moduleDeps ++ Seq(jsonSchema(crossVersion).test)
    }

  }

}