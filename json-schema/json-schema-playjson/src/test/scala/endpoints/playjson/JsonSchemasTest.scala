package endpoints.playjson

import play.api.libs.json._
import org.scalatest.freespec.AnyFreeSpec

class JsonSchemasTest extends AnyFreeSpec {

  object JsonSchemasCodec
      extends endpoints.algebra.JsonSchemasFixtures
      with endpoints.playjson.JsonSchemas

  import JsonSchemasCodec._

  "invalid json" in {
    val jsonSchema = field[Int]("relevant")
    val json = Json.obj("irrelevant" -> JsNumber(0))

    assertError(jsonSchema, json, "error.path.missing")
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
      field[BigDecimal]("foo") zip field[Boolean]("bar") zip field[Double](
        "pi"
      ),
      Json.obj(
        "foo" -> JsNumber(BigDecimal(123.456)),
        "bar" -> JsBoolean(true),
        "pi" -> JsNumber(3.1416)
      ),
      (BigDecimal(123.456), true, 3.1416)
    )
  }

  "case class with one field" in {
    case class IntClass(i: Int)
    val jsonSchemaTestClass =
      field[Int]("i").xmap[IntClass](i => IntClass(i))(_.i)

    testRoundtrip(
      jsonSchemaTestClass,
      Json.obj("i" -> JsNumber(1)),
      IntClass(1)
    )
  }

  "case class with two fields" in {
    case class TestClass(i: Int, s: String)
    val jsonSchemaTestClass = (field[Int]("i") zip field[String]("s"))
      .xmap[TestClass](tuple => TestClass(tuple._1, tuple._2))(test =>
        (test.i, test.s)
      )

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
    val input =
      Json.obj("relevant" -> Json.arr(JsString("hi"), JsBoolean(false)))

    assertError(jsonSchema, input, "error.expected.jsstring")
  }

  "map with string key" in {
    testRoundtrip(
      field[Map[String, Boolean]]("relevant"),
      Json.obj(
        "relevant" -> Json
          .obj("no" -> JsBoolean(false), "yes" -> JsBoolean(true))
      ),
      Map("no" -> false, "yes" -> true)
    )
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
    val schema = field[Double]("r")
      .tagged("Circle") orElse (field[Int]("w") zip field[Int]("h"))
      .tagged("Rect")

    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("Circle"), "r" -> JsNumber(2.0)),
      Left(2.0)
    )
    testRoundtrip(
      schema,
      Json.obj(
        "type" -> JsString("Rect"),
        "w" -> JsNumber(3),
        "h" -> JsNumber(4)
      ),
      Right((3, 4))
    )
  }

  "sealed trait" in {
    sealed trait Shape
    case class Circle(r: Double) extends Shape
    case class Rect(w: Int, h: Int) extends Shape

    val schema = field[Double]("r").tagged("Circle").xmap(Circle)(_.r) orElse
      (field[Int]("w") zip field[Int]("h"))
        .tagged("Rect")
        .xmap(Rect.tupled)(rect => (rect.w, rect.h))

    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("Circle"), "r" -> JsNumber(2.0)),
      Left(Circle(2.0))
    )
    testRoundtrip(
      schema,
      Json.obj(
        "type" -> JsString("Rect"),
        "w" -> JsNumber(3),
        "h" -> JsNumber(4)
      ),
      Right(Rect(3, 4))
    )
  }

  "sealed trait tagged merge" in {
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
    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("Circle"), "r" -> JsNumber(2.0)),
      Circle(2.0)
    )
    testRoundtrip(
      schema,
      Json.obj(
        "type" -> JsString("Rect"),
        "w" -> JsNumber(3),
        "h" -> JsNumber(4)
      ),
      Rect(3, 4)
    )
  }

  "sealed trait with xmap and tagged swapped" in {
    sealed trait Shape
    case class Circle(r: Double) extends Shape
    case class Rect(w: Int, h: Int) extends Shape

    val schema = field[Double]("r").xmap(Circle)(_.r).tagged("Circle") orElse
      (field[Int]("w") zip field[Int]("h"))
        .xmap(Rect.tupled)(rect => (rect.w, rect.h))
        .tagged("Rect")

    testRoundtrip(
      schema,
      Json.obj("type" -> JsString("Circle"), "r" -> JsNumber(2.0)),
      Left(Circle(2.0))
    )
    testRoundtrip(
      schema,
      Json.obj(
        "type" -> JsString("Rect"),
        "w" -> JsNumber(3),
        "h" -> JsNumber(4)
      ),
      Right(Rect(3, 4))
    )
  }

  "sealed trait errors" in {
    sealed trait Shape
    case class Circle(r: Double) extends Shape
    case class Rect(w: Int, h: Int) extends Shape

    val schema = field[Double]("r").xmap(Circle)(_.r).tagged("Circle") orElse
      (field[Int]("w") zip field[Int]("h"))
        .xmap(Rect.tupled)(rect => (rect.w, rect.h))
        .tagged("Rect")

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
      "Invalid value: yellow ; valid values are: Red, Blue"
    )
  }

  "enum decoding fails because value is not possible" in {
    assertError(
      colorSchema,
      JsString("Green"),
      "Invalid value: Green ; valid values are: Red, Blue"
    )
  }

  "enum decoding and encoding works" in {
    testRoundtrip(
      colorSchema,
      JsString("Blue"),
      Blue
    )
  }

  "non-string enum" in {
    import NonStringEnum.{enumSchema, Foo}
    testRoundtrip(
      enumSchema,
      Json.obj("quux" -> "bar"),
      Foo("bar")
    )
    assertError(
      enumSchema,
      Json.obj("quux" -> "wrong"),
      "Invalid value: {\"quux\":\"wrong\"} ; valid values are: {\"quux\":\"bar\"}, {\"quux\":\"baz\"}"
    )
  }

  "recursive type" in {
    testRoundtrip(
      recursiveSchema,
      Json.obj("next" -> Json.obj("next" -> Json.obj())),
      Recursive(Some(Recursive(Some(Recursive(None)))))
    )
  }

  "tuple" in {
    testRoundtrip(
      boolIntString,
      Json.arr(true, 42, "foo"),
      (true, 42, "foo")
    )
  }

  "refined JsonSchema" in {
    testRoundtrip(
      evenNumberSchema,
      JsNumber(42),
      42
    )
    assert(evenNumberSchema.writes.writes(41) == JsNumber(41))
    assertError(evenNumberSchema, JsNumber(41), "Invalid even integer '41'")
  }

  "refined Tagged" in {
    testRoundtrip(
      refinedTaggedSchema,
      Json.obj("type" -> "Baz", "i" -> 42),
      RefinedTagged(42)
    )
    assertError(
      refinedTaggedSchema,
      Json.obj("type" -> "Bar", "s" -> "foo"),
      "Invalid tagged alternative"
    )
  }

  "oneOf" in {
    testRoundtrip(intOrBoolean, JsNumber(42), Left(42))
    testRoundtrip(intOrBoolean, JsBoolean(true), Right(true))
    assertError(intOrBoolean, JsString("foo"), "Invalid value: \"foo\"")
  }

  private def testRoundtrip[A](
      jsonSchema: JsonSchema[A],
      json: JsValue,
      expected: A
  ) = {
    val result = jsonSchema.reads.reads(json)
    assert(result.isSuccess, result)
    val deserialized: A = result.get
    assert(deserialized == expected)

    val serialized: JsValue = jsonSchema.writes.writes(deserialized)
    assert(serialized == json)
  }

  private def assertError[A](
      jsonSchema: JsonSchema[A],
      json: JsValue,
      expectedError: String
  ) = {
    val jsResult = jsonSchema.reads.reads(json)
    assert(jsResult.isError)

    val errorMessages: scala.collection.Seq[String] = jsResult.asEither.left.get
      .flatMap(_._2)
      .flatMap(_.messages) // ignoring JsPath
    assert(errorMessages.head == expectedError)
  }

}
