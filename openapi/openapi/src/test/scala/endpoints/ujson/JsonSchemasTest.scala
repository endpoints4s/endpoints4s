package endpoints.ujson

import endpoints.algebra
import org.scalatest.FreeSpec

class JsonSchemasTest extends FreeSpec {

  object JsonSchemasCodec extends algebra.JsonSchemasTest with endpoints.ujson.JsonSchemas
  import JsonSchemasCodec._

  "empty record" in {
    assert(emptyRecord.codec.encode(()) == ujson.Obj())
  }

  "single record" in {
    val schema = field[String]("field1")
    assert(schema.codec.encode("string1") == ujson.Obj("field1" -> ujson.Str("string1")))
  }

  "optional field" in {
    val schema = optField[Int]("x")
    assert(schema.codec.encode(Some(42)) == ujson.Obj("x" -> ujson.Num(42)))
    assert(schema.codec.encode(None) == ujson.Obj())
  }

  "nested optional field" in {
    val schema = optField[Int]("level1")(field[Int]("level2"))
    assert(schema.codec.encode(Some(123)) == ujson.Obj("level1" -> ujson.Obj("level2" -> ujson.Num(123))))
    assert(schema.codec.encode(None) == ujson.Obj())
  }

  "two records" in {
    val schema = field[Long]("foo") zip field[Boolean]("bar")
    assert(schema.codec.encode((123L, true)) == ujson.Obj("foo" -> ujson.Num(123L), "bar" -> ujson.True))
  }

  "three records" in {
    val schema =
      field[BigDecimal]("foo") zip field[Boolean]("bar") zip field[Double]("pi")
    val expected =
      ujson.Obj(
        "foo" -> ujson.Num(123.456),
        "bar" -> ujson.True,
        "pi"  -> ujson.Num(3.1416)
      )
    assert(schema.codec.encode((BigDecimal(123.456), true, 3.1416)) == expected)
  }

  "case class with one field" in {
    case class IntClass(i: Int)
    val schema = field[Int]("i").xmap[IntClass](i => IntClass(i))(_.i)
    assert(schema.codec.encode(IntClass(1)) == ujson.Obj("i" -> ujson.Num(1)))
  }

  "case class with two fields" in {
    case class TestClass(i: Int, s: String)
    val schema = (field[Int]("i") zip field[String]("s"))
      .xmap[TestClass](tuple => TestClass(tuple._1, tuple._2))(test => (test.i, test.s))

    val expected = ujson.Obj("i" -> ujson.Num(1), "s" -> ujson.Str("one"))
    assert(schema.codec.encode(TestClass(1, "one")) == expected)
  }

  "array" in {
    val schema = field[List[String]]("names")
    val expected = ujson.Obj("names" -> ujson.Arr(ujson.Str("Ernie"), ujson.Str("Bert")))
    assert(schema.codec.encode(List("Ernie", "Bert")) == expected)
  }

  "tuple" in {
    assert(boolIntString.codec.encode((true, 42, "foo")) == ujson.Arr(ujson.True, ujson.Num(42), ujson.Str("foo")))
  }

  "map with string key" in {
    val schema = field[Map[String, Boolean]]("relevant")
    val expected =
      ujson.Obj("relevant" -> ujson.Obj("no" -> ujson.False, "yes" -> ujson.True))
    assert(schema.codec.encode(Map("no" -> false, "yes" -> true)) == expected)
  }

  "two tagged choices" in {
    val schema = field[Int]("i").tagged("I") orElse
      field[String]("s").tagged("S")
    assert(schema.codec.encode(Left(2)) == ujson.Obj("type" -> ujson.Str("I"), "i" -> ujson.Num(2)))
    assert(schema.codec.encode(Right("string")) == ujson.Obj("type" -> ujson.Str("S"), "s" -> ujson.Str("string")))
  }

  "two tagged choices with a custom discriminator" in {
    val schema =
      field[Int]("i").tagged("I")
        .orElse(field[String]("s").tagged("S"))
        .withDiscriminator("kind")
    assert(schema.codec.encode(Left(2)) == ujson.Obj("kind" -> ujson.Str("I"), "i" -> ujson.Num(2)))
    assert(schema.codec.encode(Right("string")) == ujson.Obj("kind" -> ujson.Str("S"), "s" -> ujson.Str("string")))
  }

  "enum" in {
    assert(Enum.colorSchema.codec.encode(Enum.Blue) == ujson.Str("Blue"))
  }

  "non-string enum" in {
    assert(NonStringEnum.enumSchema.codec.encode(NonStringEnum.Foo("bar")) == ujson.Obj("quux" -> ujson.Str("bar")))
  }

  "recursive type" in {
    val expected =
      ujson.Obj("next" -> ujson.Obj("next" -> ujson.Obj()))
    assert(recursiveSchema.codec.encode(Recursive(Some(Recursive(Some(Recursive(None)))))) == expected)
  }

  "refined JsonSchema" in {
    assert(evenNumberSchema.codec.encode(42) == ujson.Num(42))
  }

  "refined Tagged" in {
    val expected = ujson.Obj("type" -> ujson.Str("Baz"), "i" -> ujson.Num(42))
    assert(refinedTaggedSchema.codec.encode(RefinedTagged(42)) == expected)
  }

}
