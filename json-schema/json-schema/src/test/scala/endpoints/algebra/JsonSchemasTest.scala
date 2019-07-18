package endpoints.algebra

/**
  * This file doesn’t contain actual tests.
  *
  * Its purpose is just to exercise the [[JsonSchemas]] algebra.
  *
  * Its content can be used as fixtures by [[JsonSchemas]] interpreters.
  */
trait JsonSchemasTest extends JsonSchemas {

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
    )
      .xmap[(String, Int)](p => (p._1._2, p._2))(p => (((), p._1), p._2))
      .xmap((User.apply _).tupled)(Function.unlift(User.unapply))
  }

  sealed trait Foo
  case class Bar(s: String) extends Foo
  case class Baz(i: Int) extends Foo
  case class Bax() extends Foo
  case object Qux extends Foo
  case class Quux(b: Byte) extends Foo

  object Foo {
    implicit val schema: JsonSchema[Foo] = {
      val barSchema: Record[Bar] = field[String]("s").xmap(Bar)(_.s)
      val bazSchema: Record[Baz] = field[Int]("i").xmap(Baz)(_.i)
      val baxSchema: Record[Bax] = emptyRecord.xmap(_ => Bax())(_ => ())
      val quxSchema: Record[Qux.type] = emptyRecord.xmap(_ => Qux)(_ => ())
      val quuxSchema: Record[Quux] = field[Byte]("b").xmap(Quux)(_.b)

      (barSchema.tagged("Bar") orElse bazSchema.tagged("Baz") orElse baxSchema.tagged("Bax") orElse quxSchema.tagged("Qux") orElse quuxSchema.tagged("Quux"))
        .xmap[Foo] {
          case Left(Left(Left(Left(bar)))) => bar
          case Left(Left(Left(Right(baz)))) => baz
          case Left(Left(Right(bax))) => bax
          case Left(Right(qux)) => qux
          case Right(quux) => quux
        } {
          case bar: Bar => Left(Left(Left(Left(bar))))
          case baz: Baz => Left(Left(Left(Right(baz))))
          case bax: Bax => Left(Left(Right(bax)))
          case qux: Qux.type => Left(Right(qux))
          case quux: Quux => Right(quux)
        }
    }
  }

  object Enum {
    sealed trait Color
    case object Red extends Color
    case object Green extends Color
    case object Blue extends Color

    val colorSchema: Enum[Color] = enumeration[Color](Seq(Red, Blue))(_.toString)
  }

  case class Rec(next: Option[Rec])
  val recSchema: JsonSchema[Rec] = (
    optField("next")(lazySchema(recSchema, "Rec"))
  ).xmap(Rec)(_.next)

  val intDictionary: JsonSchema[Map[String, Int]] = mapJsonSchema[Int]

}
