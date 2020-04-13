package endpoints.openapi

import endpoints.openapi.model.OpenApi
import endpoints.{algebra, openapi}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DescriptionsTest extends AnyWordSpec with Matchers {

  "Schemas" should {

    "Include descriptions in documentation" in new Fixtures {
      checkDescription(recordSchema)("a foo bar record")
      checkDescription(coprodSchema)("a foo or a bar coprod")
      checkDescription(enumSchema)("a foo or a bar enum")
      checkDescription(arraySchema)("a list of ints")
      checkDescription(mapSchema)("a map of ints")
      checkDescription(pairSchema)("a pair of int and string")
      checkDescription(hexSchema)("a hex string")
      checkDescription(fallbackSchema)("a foo or 1 or 4")
    }

  }

  trait FixturesAlg extends algebra.JsonSchemas {

    override def defaultDiscriminatorName: String = "kind"

    val recordSchema = (
      field[String]("foo") zip
        field[Int]("bar")
    ).withDescription("a foo bar record")

    val coprodSchema = {
      val left = field[String]("foo").tagged("L")
      val right = field[Int]("bar").tagged("R")
      left.orElse(right).withDescription("a foo or a bar coprod")
    }

    val enumSchema =
      stringEnumeration(Seq("foo", "bar"))(identity)
        .withDescription("a foo or a bar enum")

    val arraySchema =
      arrayJsonSchema[List, Int].withDescription("a list of ints")

    val mapSchema = mapJsonSchema[Int].withDescription("a map of ints")

    val pairSchema =
      implicitly[JsonSchema[(Int, String)]]
        .withDescription("a pair of int and string")

    val hexSchema =
      stringJsonSchema(Some("hex")).withDescription("a hex string")

    val fallbackSchema =
      defaultStringJsonSchema
        .orFallbackTo(longJsonSchema)
        .withDescription("a foo or 1 or 4")
  }

  trait Fixtures
      extends FixturesAlg
      with openapi.Endpoints
      with openapi.JsonEntitiesFromSchemas {

    def checkDescription[A](schema: JsonSchema[A])(description: String) = {
      assert(
        OpenApi.schemaJson(toSchema(schema.docs))("description") == ujson
          .Str(description)
      )
    }

  }

}
