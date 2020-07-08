package endpoints4s.openapi

import endpoints4s.openapi.model.OpenApi
import endpoints4s.{algebra, openapi}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ExamplesTest extends AnyWordSpec with Matchers {

  "Schemas" should {

    "Include examples in documentation" in new Fixtures {
      checkExample(recordSchema)(
        ujson.Obj("foo" -> ujson.Str("Quux"), "bar" -> ujson.Num(42))
      )
      checkExample(coprodSchema)(
        ujson.Obj("kind" -> ujson.Str("R"), "bar" -> ujson.Num(42))
      )
      checkExample(enumSchema)(ujson.Str("foo"))
      checkExample(arraySchema)(ujson.Arr(ujson.Num(1), ujson.Num(2)))
      checkExample(mapSchema)(
        ujson.Obj("foo" -> ujson.Num(1), "bar" -> ujson.Num(2))
      )
      checkExample(pairSchema)(ujson.Arr(ujson.Num(42), ujson.Str("foo")))
      checkExample(hexSchema)(ujson.Str("deadbeef"))
      checkExample(fallbackSchema)(ujson.Num(1))
    }

  }

  trait FixturesAlg extends algebra.JsonSchemas {

    override def defaultDiscriminatorName: String = "kind"

    val recordSchema = (
      field[String]("foo") zip
        field[Int]("bar")
    ).withExample(("Quux", 42))

    val coprodSchema = {
      val left = field[String]("foo").tagged("L")
      val right = field[Int]("bar").tagged("R")
      left.orElse(right).withExample(Right(42))
    }

    val enumSchema =
      stringEnumeration(Seq("foo", "bar"))(identity).withExample("foo")

    val arraySchema = arrayJsonSchema[List, Int].withExample(1 :: 2 :: Nil)

    val mapSchema = mapJsonSchema[Int].withExample(Map("foo" -> 1, "bar" -> 2))

    val pairSchema =
      implicitly[JsonSchema[(Int, String)]].withExample((42, "foo"))

    val hexSchema =
      stringJsonSchema(Some("hex")).withExample("deadbeef")

    val fallbackSchema =
      defaultStringJsonSchema
        .orFallbackTo(longJsonSchema)
        .withExample(Right(1L))
  }

  trait Fixtures extends FixturesAlg with openapi.Endpoints with openapi.JsonEntitiesFromSchemas {

    def checkExample[A](schema: JsonSchema[A])(example: ujson.Value) = {
      assert(OpenApi.schemaJson(toSchema(schema.docs))("example") == example)
    }

  }

}
