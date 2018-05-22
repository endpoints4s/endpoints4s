import $file.common
import common.{EndpointsModule, EndpointsJvmModule, EndpointsJsModule, EndpointsGroupingModule, `scala 2.11 to 2.12`}
import mill._
import mill.scalalib._
import ammonite.ops.up
import mill.scalajslib.{ScalaJSModule, _}


trait OpenapiModule extends Module {

  def algebra: EndpointsGroupingModule
  def algebraCirce: EndpointsGroupingModule
  def jsonSchema: EndpointsGroupingModule

  object openapi extends EndpointsGroupingModule{
    trait Module extends EndpointsModule {
      override def artifactName = s"endpoints-openapi"
      override def millSourcePath = super.millSourcePath / up / "openapi"

      override def crossModuleDeps = Seq(algebra)

      override def ivyDeps = Agg(ivy"io.circe::circe-core::$circeVersion")

      trait OpenapiTests extends EndpointsTests {
        override def crossModuleDeps = Seq(algebra)
      }
    }

    class Jvm(val crossVersion: String) extends Module with EndpointsJvmModule {
      object test extends OpenapiTests with EndpointsJvmTests
    }
    class Js(val crossVersion: String) extends Module with EndpointsJsModule {
      object test extends OpenapiTests with EndpointsJsTests
    }

    object js extends Cross[Js](crossVersions: _*)
    object jvm extends Cross[Jvm](crossVersions: _*)
  }

  object jsonSchemaCirce extends EndpointsGroupingModule {
    trait Module extends EndpointsModule {
      override def artifactName = s"endpoints-json-schema-circe"
      override def millSourcePath = super.millSourcePath / up / "json-schema-circe"

      override def crossModuleDeps = Seq(algebra, algebraCirce)

      override def ivyDeps = super.ivyDeps() ++
        Agg(ivy"io.circe::circe-core::$circeVersion")

      trait JsonSchemaCirceTest extends EndpointsTests {
        override def ivyDeps = super.ivyDeps() ++
          Agg(ivy"io.circe::circe-generic:$circeVersion")
        override def crossModuleDeps = Seq(jsonSchema)
      }
    }

    class Jvm(val crossVersion: String) extends Module with EndpointsJvmModule {
      object test extends JsonSchemaCirceTest with EndpointsJvmTests
    }
    class Js(val crossVersion: String) extends Module with EndpointsJsModule {
      object test extends JsonSchemaCirceTest with EndpointsJsTests
    }

    object js extends Cross[Js](crossVersions: _*)
    object jvm extends Cross[Jvm](crossVersions: _*)
  }

  object jsonSchemaGeneric extends EndpointsGroupingModule {
    trait Module extends EndpointsModule {
      override def artifactName = s"endpoints-json-schema-generic"
      override def millSourcePath = super.millSourcePath / up / "json-schema-generic"

      override def crossModuleDeps = Seq(jsonSchema)

      override def ivyDeps = super.ivyDeps() ++
        Agg(ivy"com.chuusai::shapeless::2.3.2")

      trait JsonSchemaGenericTest extends EndpointsTests {
        def crossModuleDeps = Seq(openapi)
      }
    }

    class Jvm(val crossVersion: String) extends Module with EndpointsJvmModule {
      object test extends JsonSchemaGenericTest with EndpointsJvmTests
    }
    class Js(val crossVersion: String) extends Module with EndpointsJsModule {
      object test extends JsonSchemaGenericTest with EndpointsJsTests
    }

    def crossVersions: Seq[String] = `scala 2.11 to 2.12`
    object js extends Cross[Js](crossVersions: _*)
    object jvm extends Cross[Jvm](crossVersions: _*)
  }

}