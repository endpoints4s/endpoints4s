package endpoints.ujson

import java.util.UUID

import endpoints.{Invalid, Valid, algebra}
import org.scalatest.freespec.AnyFreeSpec

class JsonSchemasTest extends AnyFreeSpec {

  object JsonSchemasCodec
      extends algebra.JsonSchemasFixtures
      with endpoints.ujson.JsonSchemas
  import JsonSchemasCodec._

  "invalid records" in {
    checkDecodingFailure(
      field[Int]("foo"),
      ujson.True,
      "Invalid JSON object: true" :: Nil
    )
    checkDecodingFailure(
      field[Int]("foo"),
      ujson.Obj("bar" -> ujson.True),
      "Missing property 'foo' in JSON object: {\"bar\":true}" :: Nil
    )
    checkDecodingFailure(
      field[Int]("foo"),
      ujson.Obj("foo" -> ujson.True),
      "Invalid integer value: true" :: Nil
    )
  }

  "optional field" in {
    checkRoundTrip(
      optField[Int]("x"),
      ujson.Obj("x" -> ujson.Num(42)),
      Some(42)
    )
    checkRoundTrip(
      optField[Int]("x"),
      ujson.Obj(),
      None
    )
  }

  "invalid optional field" in {
    checkDecodingFailure(
      optField[Int]("x"),
      ujson.Obj("x" -> ujson.Str("foo")),
      "Invalid integer value: \"foo\"" :: Nil
    )
  }

  "nested optional field" in {
    checkRoundTrip(
      optField[Int]("level1")(field[Int]("level2")),
      ujson.Obj("level1" -> ujson.Obj("level2" -> ujson.Num(123))),
      Some(123)
    )
    checkRoundTrip(
      optField[Int]("level1")(field[Int]("level2")),
      ujson.Obj(),
      None
    )
  }

  "nested optional field 2" in {
    checkRoundTrip(
      field("level1")(optField[Int]("level2")),
      ujson.Obj("level1" -> ujson.Obj("level2" -> ujson.Num(123))),
      Some(123)
    )
    checkRoundTrip(
      field("level1")(optField[Int]("level2")),
      ujson.Obj("level1" -> ujson.Obj()),
      None
    )
  }

  "two records" in {
    checkRoundTrip(
      field[Long]("foo") zip field[Boolean]("bar"),
      ujson.Obj("foo" -> ujson.Num(123L), "bar" -> ujson.True),
      (123L, true)
    )
  }

  "three records" in {
    checkRoundTrip(
      field[BigDecimal]("foo") zip field[Boolean]("bar") zip field[Double](
        "pi"
      ),
      ujson.Obj(
        "foo" -> ujson.Num(123.456),
        "bar" -> ujson.True,
        "pi" -> ujson.Num(3.1416)
      ),
      (BigDecimal(123.456), true, 3.1416)
    )
  }

  "several errors" in {
    checkDecodingFailure(
      field[Int]("foo") zip field[Boolean]("bar"),
      ujson.Obj("foo" -> ujson.Str("quux")),
      "Invalid integer value: \"quux\"" :: "Missing property 'bar' in JSON object: {\"foo\":\"quux\"}" :: Nil
    )
  }

  "case class with one field" in {
    case class IntClass(i: Int)
    checkRoundTrip(
      field[Int]("i").xmap[IntClass](i => IntClass(i))(_.i),
      ujson.Obj("i" -> ujson.Num(1)),
      IntClass(1)
    )
  }

  "case class with two fields" in {
    case class TestClass(i: Int, s: String)
    checkRoundTrip(
      (field[Int]("i") zip field[String]("s"))
        .xmap[TestClass](tuple => TestClass(tuple._1, tuple._2))(test =>
          (test.i, test.s)
        ),
      ujson.Obj("i" -> ujson.Num(1), "s" -> ujson.Str("one")),
      TestClass(1, "one")
    )
  }

  "array" in {
    checkRoundTrip(
      field[List[String]]("names"),
      ujson.Obj("names" -> ujson.Arr(ujson.Str("Ernie"), ujson.Str("Bert"))),
      List("Ernie", "Bert")
    )
    checkRoundTrip(
      field[List[String]]("names"),
      ujson.Obj("names" -> ujson.Arr()),
      List()
    )
    checkDecodingFailure(
      arrayJsonSchema[List, Int],
      ujson.Obj(),
      "Invalid JSON array: {}" :: Nil
    )
    checkDecodingFailure(
      arrayJsonSchema[List, Int],
      ujson.Arr(ujson.Num(0), ujson.Str("foo"), ujson.Num(3.14)),
      "Invalid integer value: \"foo\"" :: "Invalid integer value: 3.14" :: Nil
    )
  }

  "tuple" in {
    checkRoundTrip(
      boolIntString,
      ujson.Arr(ujson.True, ujson.Num(42), ujson.Str("foo")),
      (true, 42, "foo")
    )
    checkDecodingFailure(
      boolIntString,
      ujson.Arr(ujson.True, ujson.Str("foo"), ujson.Num(42)),
      "Invalid integer value: \"foo\"" :: "Invalid string value: 42" :: Nil
    )
    checkDecodingFailure(
      boolIntString,
      ujson.Arr(ujson.True),
      "Invalid JSON array of 3 elements: [true]" :: Nil
    )
  }

  "map with string key" in {
    checkRoundTrip(
      mapJsonSchema[Boolean],
      ujson.Obj("no" -> ujson.False, "yes" -> ujson.True),
      Map("no" -> false, "yes" -> true)
    )
    checkDecodingFailure(
      mapJsonSchema[Boolean],
      ujson.Obj("foo" -> ujson.Num(42)),
      "Invalid boolean value: 42" :: Nil
    )
  }

  "two tagged choices" in {
    val schema = field[Int]("i").tagged("I") orElse
      field[String]("s").tagged("S")
    checkRoundTrip(
      schema,
      ujson.Obj("type" -> ujson.Str("I"), "i" -> ujson.Num(2)),
      Left(2)
    )
    checkRoundTrip(
      schema,
      ujson.Obj("type" -> ujson.Str("S"), "s" -> ujson.Str("string")),
      Right("string")
    )
    checkDecodingFailure(
      schema,
      ujson.Arr(),
      "Invalid JSON object: []" :: Nil
    )
    checkDecodingFailure(
      schema,
      ujson.Obj("s" -> ujson.Str("string")),
      "Missing type discriminator property 'type': {\"s\":\"string\"}" :: Nil
    )
    checkDecodingFailure(
      schema,
      ujson.Obj("type" -> ujson.Str("B"), "s" -> ujson.Str("string")),
      "Invalid type discriminator: 'B'" :: Nil
    )
    checkDecodingFailure(
      schema,
      ujson.Obj("type" -> ujson.Str("I"), "s" -> ujson.Str("string")),
      "Missing property 'i' in JSON object: {\"type\":\"I\",\"s\":\"string\"}" :: Nil
    )
  }

  "two tagged choices with a custom discriminator" in {
    val schema =
      field[Int]("i")
        .tagged("I")
        .orElse(field[String]("s").tagged("S"))
        .withDiscriminator("kind")
        .xmap(identity)(
          identity
        ) // Make sure that `xmap` preserves the discriminator
    checkRoundTrip(
      schema,
      ujson.Obj("kind" -> ujson.Str("I"), "i" -> ujson.Num(2)),
      Left(2)
    )
    checkRoundTrip(
      schema,
      ujson.Obj("kind" -> ujson.Str("S"), "s" -> ujson.Str("string")),
      Right("string")
    )
  }

  "two tagged choices using orElseMerged" in {
    sealed trait Shape
    case class Circle(r: Double) extends Shape
    case class Rect(w: Int, h: Int) extends Shape

    val circleSchema = field[Double]("r")
      .tagged("Circle")
      .xmap(Circle)(_.r)

    val rectSchema = (field[Int]("w") zip field[Int]("h"))
      .tagged("Rect")
      .xmap(Rect.tupled)(rect => (rect.w, rect.h))

    val schema = circleSchema orElseMerge rectSchema
    checkRoundTrip(
      schema,
      ujson.Obj("type" -> ujson.Str("Circle"), "r" -> ujson.Num(2.3)),
      Circle(2.3)
    )
    checkRoundTrip(
      schema,
      ujson.Obj(
        "type" -> ujson.Str("Rect"),
        "w" -> ujson.Num(1),
        "h" -> ujson.Num(2)
      ),
      Rect(1, 2)
    )
    checkDecodingFailure(
      schema,
      ujson.Arr(),
      "Invalid JSON object: []" :: Nil
    )
    checkDecodingFailure(
      schema,
      ujson.Obj("s" -> ujson.Str("string")),
      "Missing type discriminator property 'type': {\"s\":\"string\"}" :: Nil
    )
    checkDecodingFailure(
      schema,
      ujson.Obj("type" -> ujson.Str("B"), "s" -> ujson.Str("string")),
      "Invalid type discriminator: 'B'" :: Nil
    )
    checkDecodingFailure(
      schema,
      ujson.Obj("type" -> ujson.Str("Circle"), "s" -> ujson.Num(1.2)),
      "Missing property 'r' in JSON object: {\"type\":\"Circle\",\"s\":1.2}" :: Nil
    )
  }

  "two tagged choices with a custom discriminator using orElseMerged" in {
    sealed trait Shape
    case class Circle(r: Double) extends Shape
    case class Rect(w: Int, h: Int) extends Shape

    val circleSchema = field[Double]("r")
      .tagged("Circle")
      .xmap(Circle)(_.r)

    val rectSchema = (field[Int]("w") zip field[Int]("h"))
      .tagged("Rect")
      .xmap(Rect.tupled)(rect => (rect.w, rect.h))

    val schema =
      (circleSchema orElseMerge rectSchema).withDiscriminator("shape")

    checkRoundTrip(
      schema,
      ujson.Obj("shape" -> ujson.Str("Circle"), "r" -> ujson.Num(2.3)),
      Circle(2.3)
    )
    checkRoundTrip(
      schema,
      ujson.Obj(
        "shape" -> ujson.Str("Rect"),
        "w" -> ujson.Num(1),
        "h" -> ujson.Num(2)
      ),
      Rect(1, 2)
    )
  }

  "enum" in {
    checkRoundTrip(
      Enum.colorSchema,
      ujson.Str("Blue"),
      Enum.Blue
    )
    checkDecodingFailure(
      Enum.colorSchema,
      ujson.Num(42),
      "Invalid string value: 42" :: Nil
    )
    checkDecodingFailure(
      Enum.colorSchema,
      ujson.Str("Orange"),
      "Invalid value: Orange ; valid values are: Red, Blue" :: Nil
    )
  }

  "non-string enum" in {
    assert(
      NonStringEnum.enumSchema.codec.encode(NonStringEnum.Foo("bar")) == ujson
        .Obj("quux" -> ujson.Str("bar"))
    )
  }

  "recursive type" in {
    checkRoundTrip(
      recursiveSchema,
      ujson.Obj("next" -> ujson.Obj("next" -> ujson.Obj())),
      Recursive(Some(Recursive(Some(Recursive(None)))))
    )
  }

  "refined JsonSchema" in {
    checkRoundTrip(
      evenNumberSchema,
      ujson.Num(42),
      42
    )
    checkDecodingFailure(
      evenNumberSchema,
      ujson.Num(41),
      "Invalid even integer '41'" :: Nil
    )
  }

  "refined Tagged" in {
    checkRoundTrip(
      refinedTaggedSchema,
      ujson.Obj("type" -> ujson.Str("Baz"), "i" -> ujson.Num(42)),
      RefinedTagged(42)
    )
    checkDecodingFailure(
      refinedTaggedSchema,
      ujson.Obj("type" -> ujson.Str("Bar"), "s" -> ujson.Str("hello")),
      "Invalid tagged alternative" :: Nil
    )
  }

  "uuid" in {
    val uuid = UUID.randomUUID()
    checkRoundTrip(
      uuidJsonSchema,
      ujson.Str(uuid.toString),
      uuid
    )
    checkDecodingFailure(
      uuidJsonSchema,
      ujson.Str("foo"),
      "Invalid UUID value: 'foo'" :: Nil
    )
  }

  "oneOf" in {
    checkRoundTrip(intOrBoolean, ujson.Num(42), Left(42))
    checkRoundTrip(intOrBoolean, ujson.Bool(true), Right(true))
    checkDecodingFailure(
      intOrBoolean,
      ujson.Str("foo"),
      Seq("Invalid value: \"foo\"")
    )
  }

  def checkRoundTrip[A](
      schema: JsonSchema[A],
      json: ujson.Value,
      expected: A
  ): Unit = {
    schema.codec.decode(json) match {
      case Valid(decoded)  => assert(decoded == expected)
      case Invalid(errors) => fail(errors.toString())
    }
    val encoded = schema.codec.encode(expected)
    assert(encoded == json)
  }

  def checkDecodingFailure[A](
      schema: JsonSchema[A],
      json: ujson.Value,
      expectedErrors: Seq[String]
  ): Unit =
    schema.codec.decode(json) match {
      case Valid(_)        => fail("Expected decoding failure")
      case Invalid(errors) => assert(errors == expectedErrors)
    }

}
