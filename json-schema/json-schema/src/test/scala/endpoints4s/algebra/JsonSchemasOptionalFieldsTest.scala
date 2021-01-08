package endpoints4s.algebra

import java.time.{Duration, Instant, OffsetDateTime, ZoneOffset}

import endpoints4s.{Invalid, Valid, Validated}
import org.scalatest.freespec.AnyFreeSpec

/** Tests that must be run on all [[JsonSchemas]] interpreters.
  */
trait JsonSchemasOptionalFieldsTest extends AnyFreeSpec with JsonSchemasFixtures {

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
      case Invalid(errors) => fail(errors.toString())
    }
    val encoded = encodeJson(schema, 1)
    assert(encoded == Json.obj("relevant" -> Json.num(1)))
  }

  "encoding and decoding optional field with default value, when field is present" in {
    val schema = optFieldWithDefault[Int]("value", 42)
    checkRoundTrip(
      schema,
      Json.obj("value" -> Json.num(1)),
      1
    )
  }

  "decoding optional field with default value fallbacks to default value when absent" in {
    val schema = optFieldWithDefault[Int]("value", 42)
    assert(decodeJson(schema, Json.obj()) == Valid(42))
  }

  "encoding optional field with default value always emits the field" in {
    val schema = optFieldWithDefault[Int]("value", 42)
    assert(encodeJson(schema, 42) == Json.obj("value" -> Json.num(42)))
  }

  "decoding optional field with default value fails if field if present but invalid" in {
    val schema = optFieldWithDefault[Int]("value", 42)
    val invalidJson = Json.obj("value" -> Json.str("one"))
    assert(decodeJson(schema, invalidJson).isInstanceOf[Invalid])
  }

  "Instant" in {
    val now = Instant.now()
    checkRoundTrip[Instant](implicitly, Json.str(now.toString), now)
    val offsetDateTime = OffsetDateTime.of(2020, 10, 29, 10, 28, 0, 0, ZoneOffset.ofHours(2))
    checkDecodingFailure[Instant](
      implicitly,
      Json.str(offsetDateTime.toString),
      Seq(s"Text '${offsetDateTime.toString}' could not be parsed at index 16")
    )
  }

  "OffsetDateTime" in {
    val now = OffsetDateTime.now()
    checkRoundTrip[OffsetDateTime](implicitly, Json.str(now.toString), now)
    checkDecodingFailure[OffsetDateTime](
      implicitly,
      Json.str("not a date"),
      Seq("Text 'not a date' could not be parsed at index 0")
    )
  }

  "Duration" in {
    val duration = Duration.ofDays(7)
    checkRoundTrip[Duration](implicitly, Json.str(duration.toString), duration)
    checkDecodingFailure[Duration](
      implicitly,
      Json.str("not a duration"),
      Seq("Text cannot be parsed to a Duration")
    )
  }

  "Set" in {
    checkRoundTrip[Set[Int]](implicitly, Json.arr(Json.num(1)), Set(1))
  }

  def checkRoundTrip[A](schema: JsonSchema[A], json: Json.Json, decoded: A) =
    decodeJson(schema, json) match {
      case Valid(a) =>
        assert(a == decoded && encodeJson(schema, a) == json)
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
