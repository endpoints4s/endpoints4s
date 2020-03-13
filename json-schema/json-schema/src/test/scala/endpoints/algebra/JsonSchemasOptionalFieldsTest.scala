package endpoints.algebra

import java.util.UUID

import endpoints.{Invalid, Valid, Validated}
import org.scalatest.freespec.AnyFreeSpec

/**
  * Tests that must be run on all [[JsonSchemas]] interpreters.
  */
trait JsonSchemasOptionalFieldsTest
    extends AnyFreeSpec
    with JsonSchemasFixtures {

  // Abstract over concrete JSON library (such as circe, Play JSON, or ujson)
  trait Json {
    type Json
    def obj(fields: (String, Json)*): Json
    def arr(items: Json*): Json
    def num(x: BigDecimal): Json
    def str(s: String): Json
    def bool(b: Boolean): Json
    def `null`: Json
  }
  val Json: Json
  def decodeJson[A](schema: JsonSchema[A], json: Json.Json): Validated[A]
  def encodeJson[A](schema: JsonSchema[A], value: A): Json.Json

  "empty record" in {
    checkRoundTrip(
      emptyRecord,
      Json.obj(),
      ()
    )
  }

  "invalid empty record" in {
    val jsonSchema = emptyRecord
    val json = Json.arr()
    checkDecodingFailure(jsonSchema, json, Seq("Invalid JSON object: []"))
  }

  "missing optional field" in {
    checkRoundTrip(
      optField[Int]("relevant"),
      Json.obj(),
      None
    )
  }

  "optional field" in {
    checkRoundTrip(
      optField[Int]("relevant"),
      Json.obj("relevant" -> Json.num(123)),
      Some(123)
    )
  }

  "optional field null" in {
    val schema = optField[Int]("relevant")
    val json = Json.obj("relevant" -> Json.`null`)

    // We don’t use “testRoundtrip” here because we decode a `null` field
    // but we don’t produce that field when encoding
    decodeJson(schema, json) match {
      case Valid(None) =>
        assert(encodeJson(schema, None) == Json.obj())
      case Valid(Some(n))  => fail(s"Decoded $n")
      case Invalid(errors) => fail(errors.mkString(". "))
    }
  }

  "nested optional field" in {
    checkRoundTrip(
      optField[Int]("level1")(field[Int]("level2")),
      Json.obj("level1" -> Json.obj("level2" -> Json.num(123))),
      Some(123)
    )
  }

  "missing nested optional field" in {
    checkRoundTrip(
      optField[Int]("level1")(field[Int]("level2")),
      Json.obj(),
      None
    )
  }

  "single record" in {
    checkRoundTrip(
      field[String]("field1"),
      Json.obj("field1" -> Json.str("string1")),
      "string1"
    )
  }

  "ignore extra record fields" in {
    val schema = field[Int]("relevant")
    val json = Json.obj("relevant" -> Json.num(1), "irrelevant" -> Json.num(0))
    val decoded = decodeJson(schema, json)
    decoded match {
      case Valid(n)        => assert(n == 1)
      case Invalid(errors) => fail(errors.toString)
    }
    val encoded = encodeJson(schema, 1)
    assert(encoded == Json.obj("relevant" -> Json.num(1)))
  }

  def checkRoundTrip[A](schema: JsonSchema[A], json: Json.Json, decoded: A) =
    decodeJson(schema, json) match {
      case Valid(a) =>
        assert(encodeJson(schema, a) == json)
      case Invalid(errors) =>
        fail(errors.mkString(". "))
    }

  def checkDecodingFailure[A](
      schema: JsonSchema[A],
      json: Json.Json,
      expectedErrors: Seq[String]
  ) =
    decodeJson(schema, json) match {
      case Valid(_)        => fail("Expected decoding failure")
      case Invalid(errors) => assert(errors == expectedErrors)
    }

}
