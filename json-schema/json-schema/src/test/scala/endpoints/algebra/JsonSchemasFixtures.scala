package endpoints.algebra

import endpoints.{Invalid, Valid}

/**
  * This file doesn’t contain actual tests.
  *
  * Its purpose is just to exercise the [[JsonSchemas]] algebra.
  *
  * Its content can be used as fixtures by [[JsonSchemas]] interpreters.
  */
trait JsonSchemasFixtures extends JsonSchemas {

  case class User(name: String, age: Int)

  object User {
    implicit val schema: JsonSchema[User] = (
      field[String]("name", Some("Name of the user")) zip
        field[Int]("age")
    ).xmap((User.apply _).tupled)(Function.unlift(User.unapply))

    val schema2: JsonSchema[User] = (
      emptyRecord zip
        field[String]("name", Some("Name of the user")) zip
        field[Int]("age")
    ).xmap((User.apply _).tupled)(Function.unlift(User.unapply))
  }

  sealed trait Foo
  case class Bar(s: String) extends Foo
  case class Baz(i: Int) extends Foo
  case class Bax() extends Foo
  case object Qux extends Foo
  case class Quux(b: Byte) extends Foo

  object Foo {
    implicit val schema: Tagged[Foo] = {
      val barSchema: Record[Bar] = field[String]("s").xmap(Bar)(_.s)
      val bazSchema: Record[Baz] = field[Int]("i").xmap(Baz)(_.i)
      val baxSchema: Record[Bax] = emptyRecord.xmap(_ => Bax())(_ => ())
      val quxSchema: Record[Qux.type] = emptyRecord.xmap(_ => Qux)(_ => ())
      val quuxSchema: Record[Quux] = field[Byte]("b").xmap(Quux)(_.b)

      (barSchema.tagged("Bar") orElse bazSchema.tagged("Baz") orElse baxSchema
        .tagged("Bax") orElse quxSchema.tagged("Qux") orElse quuxSchema.tagged(
        "Quux"
      )).xmap[Foo] {
        case Left(Left(Left(Left(bar))))  => bar
        case Left(Left(Left(Right(baz)))) => baz
        case Left(Left(Right(bax)))       => bax
        case Left(Right(qux))             => qux
        case Right(quux)                  => quux
      } {
        case bar: Bar      => Left(Left(Left(Left(bar))))
        case baz: Baz      => Left(Left(Left(Right(baz))))
        case bax: Bax      => Left(Left(Right(bax)))
        case qux: Qux.type => Left(Right(qux))
        case quux: Quux    => Right(quux)
      }
    }

    val alternativeSchemaForMerge: Tagged[Foo] = {
      val barSchema: Record[Bar] = field[String]("s").xmap(Bar)(_.s)
      val bazSchema: Record[Baz] = field[Int]("i").xmap(Baz)(_.i)
      val baxSchema: Record[Bax] = emptyRecord.xmap(_ => Bax())(_ => ())
      val quxSchema: Record[Qux.type] = emptyRecord.xmap(_ => Qux)(_ => ())
      val quuxSchema: Record[Quux] = field[Byte]("b").xmap(Quux)(_.b)

      (
        barSchema.tagged("Bar") orElseMerge
          bazSchema.tagged("Baz") orElseMerge
          baxSchema.tagged("Bax") orElseMerge
          quxSchema.tagged("Qux") orElseMerge
          quuxSchema.tagged("Quux")
      )
    }
  }

  object Enum {
    sealed trait Color
    case object Red extends Color
    case object Green extends Color
    case object Blue extends Color

    val colorSchema: Enum[Color] =
      stringEnumeration[Color](Seq(Red, Blue))(_.toString).named("Color")
  }

  object NonStringEnum {
    case class Foo(quux: String)

    val fooSchema: JsonSchema[Foo] = field[String]("quux").xmap(Foo(_))(_.quux)
    val enumSchema: Enum[Foo] =
      enumeration(Seq(Foo("bar"), Foo("baz")))(fooSchema)
  }

  case class Recursive(next: Option[Recursive])
  val recursiveSchema: Record[Recursive] = (
    optField("next")(lazyRecord(recursiveSchema, "Rec"))
  ).xmap(Recursive)(_.next)

  val intDictionary: JsonSchema[Map[String, Int]] = mapJsonSchema[Int]

  implicit val boolIntString: JsonSchema[(Boolean, Int, String)] =
    tuple3JsonSchema

  //#refined
  val evenNumberSchema: JsonSchema[Int] =
    intJsonSchema.xmapPartial { n =>
      if (n % 2 == 0) Valid(n)
      else Invalid(s"Invalid even integer '$n'")
    }(n => n)
  //#refined

  case class RefinedTagged(x: Int)
  val refinedTaggedSchema: Tagged[RefinedTagged] =
    Foo.schema.xmapPartial {
      case Baz(i) => Valid(RefinedTagged(i))
      case _      => Invalid("Invalid tagged alternative")
    }(rc => Baz(rc.x))

  //#one-of
  val intOrBoolean: JsonSchema[Either[Int, Boolean]] =
    intJsonSchema.orFallbackTo(booleanJsonSchema)
  //#one-of

}
