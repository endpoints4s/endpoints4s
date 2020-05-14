package endpoints.openapi

import endpoints.openapi.model.OpenApi
import endpoints.{algebra, openapi}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TitlesTest extends AnyWordSpec with Matchers {

  "Schemas" should {

    "Include titles in documentation" in new Fixtures {
      checkTitle(recordSchema)("record")
      checkTitle(coprodSchema)("coprod")
      checkTitle(enumSchema)("enum")
      checkTitle(arraySchema)("list of ints")
      checkTitle(mapSchema)("map of ints")
      checkTitle(pairSchema)("pair")
      checkTitle(hexSchema)("hex string")
      checkTitle(fallbackSchema)("fallback literals")
    }

  }

  trait FixturesAlg extends algebra.JsonSchemas {

    override def defaultDiscriminatorName: String = "kind"

    val recordSchema = (
      field[String]("foo") zip
        field[Int]("bar")
    ).withTitle("record")

    val coprodSchema = {
      val left = field[String]("foo").tagged("L")
      val right = field[Int]("bar").tagged("R")
      left.orElse(right).withTitle("coprod")
    }

    val enumSchema =
      stringEnumeration(Seq("foo", "bar"))(identity).withTitle("enum")

    val arraySchema = arrayJsonSchema[List, Int].withTitle("list of ints")

    val mapSchema = mapJsonSchema[Int].withTitle("map of ints")

    val pairSchema =
      implicitly[JsonSchema[(Int, String)]].withTitle("pair")

    val hexSchema =
      stringJsonSchema(Some("hex")).withTitle("hex string")

    val fallbackSchema =
      defaultStringJsonSchema
        .orFallbackTo(longJsonSchema)
        .withTitle("fallback literals")
  }

  trait Fixtures
      extends FixturesAlg
      with openapi.Endpoints
      with openapi.JsonEntitiesFromSchemas {

    def checkTitle[A](schema: JsonSchema[A])(title: String) = {
      assert(
        OpenApi.schemaJson(toSchema(schema.docs))("title") == ujson.Str(title)
      )
    }

  }

}
