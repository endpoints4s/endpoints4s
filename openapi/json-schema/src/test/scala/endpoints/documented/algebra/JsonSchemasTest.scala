package endpoints.documented.algebra

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
      field[String]("name") zip
      field[Int]("age")
    ).invmap((User.apply _).tupled)(Function.unlift(User.unapply))
  }

  sealed trait Foo
  case class Bar(s: String) extends Foo
  case class Baz(i: Int) extends Foo

  object Foo {
    implicit val schema: JsonSchema[Foo] = {
      val barSchema: Record[Bar] = field[String]("s").invmap(Bar)(_.s)
      val bazSchema: Record[Baz] = field[Int]("i").invmap(Baz)(_.i)

      (barSchema.tagged("Bar") orElse bazSchema.tagged("Baz"))
        .invmap[Foo] {
          case Left(bar) => bar
          case Right(baz) => baz
        } {
          case bar: Bar => Left(bar)
          case baz: Baz => Right(baz)
        }
    }
  }

}
