package endpoints.playjson

import org.scalatest.FreeSpec
import play.api.libs.json._

class JsonSchemasTest extends FreeSpec {

  object JsonSchemasCodec
    extends endpoints.algebra.JsonSchemasTest
      with endpoints.playjson.JsonSchemas

  import JsonSchemasCodec.{JsonSchemaOps => _, _} // importing JsonSchemaOps would create ambiguity with RecordOps

  "empty record" in {
    testRoundtrip(
      emptyRecord,
      Json.obj(),
      ()
    )
  }

  "invalid empty record" in {
    val jsonSchema = emptyRecord
    val json = Json.arr()

    assertError(jsonSchema, json, "expected JSON object, but found: []")
  }

  "single record" in {
    testRoundtrip(
      field[String]("field1"),
      Json.obj("field1" -> "string1"),
      "string1"
    )
  }

  "ignore record" in {
    val jsonSchema = field[Int]("relevant")
    val input = Json.obj("relevant" -> JsNumber(1), "irrelevant" -> JsNumber(0))

    val deserialized = jsonSchema.reads.reads(input).get
    assert(deserialized == 1)

    val output = jsonSchema.writes.writes(deserialized)
    assert(output == Json.obj("relevant" -> JsNumber(1)))
  }

  "invalid json" in {
    val jsonSchema = field[Int]("relevant")
    val json = Json.obj("irrelevant" -> JsNumber(0))

    assertError(jsonSchema, json, "error.path.missing")
  }

  "missing optional field" in {
    testRoundtrip(
      optField[Int]("relevant"),
      Json.obj(),
      None
    )
  }

  "optional field" in {
    testRoundtrip(
      optField[Int]("relevant"),
      Json.obj("relevant" -> JsNumber(123)),
      Some(123)
    )
  }

  "optional field null" in {
    val jsonSchema = optField[Int]("relevant")
    val input = Json.obj("relevant" -> JsNull)

    val deserialized = jsonSchema.reads.reads(input).get
    assert(deserialized == None)

    val output = jsonSchema.writes.writes(deserialized)
    assert(output == Json.obj())
  }

  "nested optional field" in {
    testRoundtrip(
      optField[Int]("level1")(field[Int]("level2")),
      Json.obj("level1" -> Json.obj("level2" -> JsNumber(123))),
      Some(123)
    )
  }

  "missing nested optional field" in {
    testRoundtrip(
      optField[Int]("level1")(field[Int]("level2")),
      Json.obj(),
      None
    )
  }

  "nested record" in {
    testRoundtrip(
      field("level1")(field[Int]("level2")),
      Json.obj("level1" -> Json.obj("level2" -> JsNumber(123))),
      123
    )
  }

  "two records" in {
    testRoundtrip(
      field[Long]("foo") zip field[Boolean]("bar"),
      Json.obj("foo" -> JsNumber(123L), "bar" -> JsBoolean(true)),
      (123L, true)
    )
  }

  "three records" in {
    testRoundtrip(
      field[BigDecimal]("foo") zip field[Boolean]("bar") zip field[Double]("pi"),
      Json.obj("foo" -> JsNumber(BigDecimal(123.456)), "bar" -> JsBoolean(true), "pi" -> JsNumber(3.1416)),
      ((BigDecimal(123.456), true), 3.1416)
    )
  }

  "case class with one field" in {
    case class IntClass(i: Int)
    val jsonSchemaTestClass = field[Int]("i").invmap[IntClass](i => IntClass(i))(_.i)

    testRoundtrip(
      jsonSchemaTestClass,
      Json.obj("i" -> JsNumber(1)),
      IntClass(1)
    )
  }

  "case class with two fields" in {
    case class TestClass(i: Int, s: String)
    val jsonSchemaTestClass = (field[Int]("i") zip field[String]("s"))
      .invmap[TestClass](tuple => TestClass(tuple._1, tuple._2))(test => (test.i, test.s))

    testRoundtrip(
      jsonSchemaTestClass,
      Json.obj("i" -> JsNumber(1), "s" -> JsString("one")),
      TestClass(1, "one")
    )
  }

  "case class" in {
    testRoundtrip(
      User.schema,
      Json.obj("age" -> JsNumber(42), "name" -> JsString("John")),
      User("John", 42)
    )
  }

  "array" in {
    testRoundtrip(
      field[List[String]]("names"),
      Json.obj("names" -> Json.arr(JsString("Ernie"), JsString("Bert"))),
      List("Ernie", "Bert")
    )
  }

  "long array" in {
    val numbers: Seq[Int] = 0 until 10000
    val jsNumbers: Seq[JsNumber] = numbers.map(JsNumber(_))

    testRoundtrip(
      field[Seq[Int]]("numbers"),
      Json.obj("numbers" -> JsArray(jsNumbers)),
      numbers
    )
  }

  "empty array" in {
    testRoundtrip(
      field[Seq[Int]]("coords"),
      Json.obj("coords" -> Json.arr()),
      Seq()
    )
  }

  "heterogeneous array" in {
    val jsonSchema = field[String]("relevant")
    val input = Json.obj("relevant" -> Json.arr(JsString("hi"), JsBoolean(false)))

    assertError(jsonSchema, input, "error.expected.jsstring")
  }

  "tagged single record" in {
    testRoundtrip(
      field[Double]("x").tagged("Rectangle"),
      Json.obj("type" -> JsString("Rectangle"), "x" -> JsNumber(1.5)),
      1.5
    )
  }

  "two tagged choices" in {
    val schema = field[Int]("i").tagged("I") orElse
      field[String]("s").tagged("S")

    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("I"), "i" -> JsNumber(2)),
      Left(2)
    )
    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("S"), "s" -> JsString("string")),
      Right("string")
    )
  }

  "three tagged choices" in {
    val schema = field[Int]("i").tagged("I") orElse
      field[String]("s").tagged("S") orElse
      field[Boolean]("b").tagged("B")

    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("I"), "i" -> JsNumber(2)),
      Left(Left(2))
    )
    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("B"), "b" -> JsBoolean(true)),
      Right(true)
    )
  }

  "tagged and zipped" in {
    val schema = field[Double]("r").tagged("Circle") orElse (field[Int]("w") zip field[Int]("h")).tagged("Rect")

    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("Circle"), "r" -> JsNumber(2.0)),
      Left(2.0)
    )
    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("Rect"), "w" -> JsNumber(3), "h" -> JsNumber(4)),
      Right((3, 4))
    )
  }

  "sealed trait" in {
    sealed trait Shape
    case class Circle(r: Double) extends Shape
    case class Rect(w: Int, h: Int) extends Shape

    val schema = field[Double]("r").tagged("Circle").invmap(Circle)(_.r) orElse
      (field[Int]("w") zip field[Int]("h")).tagged("Rect").invmap(Rect.tupled)(rect => (rect.w, rect.h))

    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("Circle"), "r" -> JsNumber(2.0)),
      Left(Circle(2.0))
    )
    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("Rect"), "w" -> JsNumber(3), "h" -> JsNumber(4)),
      Right(Rect(3, 4))
    )
  }

  "sealed trait with invmap and tagged swapped" in {
    sealed trait Shape
    case class Circle(r: Double) extends Shape
    case class Rect(w: Int, h: Int) extends Shape

    val schema = field[Double]("r").invmap(Circle)(_.r).tagged("Circle") orElse
      (field[Int]("w") zip field[Int]("h")).invmap(Rect.tupled)(rect => (rect.w, rect.h)).tagged("Rect")

    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("Circle"), "r" -> JsNumber(2.0)),
      Left(Circle(2.0))
    )
    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("Rect"), "w" -> JsNumber(3), "h" -> JsNumber(4)),
      Right(Rect(3, 4))
    )
  }

  "sealed trait errors" in {
    sealed trait Shape
    case class Circle(r: Double) extends Shape
    case class Rect(w: Int, h: Int) extends Shape

    val schema = field[Double]("r").invmap(Circle)(_.r).tagged("Circle") orElse
      (field[Int]("w") zip field[Int]("h")).invmap(Rect.tupled)(rect => (rect.w, rect.h)).tagged("Rect")

    assertError(
      schema,
      Json.arr(),
      "expected JSON object for tagged type, but found: []"
    )
    assertError(
      schema,
      Json.obj("Circle" -> Json.obj(), "Rect" -> Json.obj()),
      """expected discriminator field 'type', but not found in: {"Circle":{},"Rect":{}}"""
    )
    assertError(
      schema,
      Json.obj("type" -> JsString("Square")),
      """no Reads for tag 'Square': {"type":"Square"}"""
    )
  }

  import Enum._

  "enum decoding fails because value cannot be decoded" in {
    assertError(
      colorSchema,
      JsString("yellow"),
      "Cannot decode as enum value: yellow"
    )
  }

  "enum decoding fails because value is not possible" in {
    assertError(
      colorSchema,
      JsString("Green"),
      "Cannot decode as enum value: Green"
    )
  }

  "enum decoding and encoding works" in {
    testRoundtrip(
      colorSchema,
      JsString("Blue"),
      Blue
    )
  }

  private def testRoundtrip[A](jsonSchema: JsonSchema[A], json: JsValue, expected: A) = {
    val result = jsonSchema.reads.reads(json)
    assert(result.isSuccess, result)
    val deserialized: A = result.get
    assert(deserialized == expected)

    val serialized: JsValue = jsonSchema.writes.writes(deserialized)
    assert(serialized == json)
  }

  private def assertError[A](jsonSchema: JsonSchema[A], json: JsValue, expectedError: String) = {
    val jsResult = jsonSchema.reads.reads(json)
    assert(jsResult.isError)

    val errorMessages: Seq[String] = jsResult.asEither.left.get.flatMap(_._2).flatMap(_.messages) // ignoring JsPath
    assert(errorMessages.head == expectedError)
  }

}
