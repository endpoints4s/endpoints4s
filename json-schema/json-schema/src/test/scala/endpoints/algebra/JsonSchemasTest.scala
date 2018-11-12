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
    ).invmap((User.apply _).tupled)(Function.unlift(User.unapply))

    val schema2: JsonSchema[User] = (
      emptyRecord zip
      field[String]("name", Some("Name of the user")) zip
      field[Int]("age")
    )
      .invmap[(String, Int)](p => (p._1._2, p._2))(p => (((), p._1), p._2))
      .invmap((User.apply _).tupled)(Function.unlift(User.unapply))
  }

  sealed trait Foo
  case class Bar(s: String) extends Foo
  case class Baz(i: Int) extends Foo
  case class Bax() extends Foo
  case object Qux extends Foo

  object Foo {
    implicit val schema: JsonSchema[Foo] = {
      val barSchema: Record[Bar] = field[String]("s").invmap(Bar)(_.s)
      val bazSchema: Record[Baz] = field[Int]("i").invmap(Baz)(_.i)
      val baxSchema: Record[Bax] = emptyRecord.invmap(_ => Bax())(_ => ())
      val quxSchema: Record[Qux.type] = emptyRecord.invmap(_ => Qux)(_ => ())

      (barSchema.tagged("Bar") orElse bazSchema.tagged("Baz") orElse baxSchema.tagged("Bax") orElse quxSchema.tagged("Qux"))
        .invmap[Foo] {
          case Left(Left(Left(bar))) => bar
          case Left(Left(Right(baz))) => baz
          case Left(Right(bax)) => bax
          case Right(qux) => qux
        } {
          case bar: Bar => Left(Left(Left(bar)))
          case baz: Baz => Left(Left(Right(baz)))
          case bax: Bax => Left(Right(bax))
          case qux: Qux.type => Right(qux)
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

}
