package endpoints4s.algebra

import endpoints4s.{Invalid, NumericConstraints, Valid}

/** This file doesn’t contain actual tests.
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
    ).xmap((User.apply _).tupled)(user => (user.name, user.age))

    val schema2: JsonSchema[User] = (
      emptyRecord zip
        field[String]("name", Some("Name of the user")) zip
        field[Int]("age")
    ).xmap((User.apply _).tupled)(user => (user.name, user.age))

    val schemaWithDefault: JsonSchema[User] = (
      field[String]("name", Some("Name of the user")) zip
        optFieldWithDefault[Int]("age", 42)
    ).xmap((User.apply _).tupled)(user => (user.name, user.age))
  }

  sealed trait Foo
  case class Bar(s: String) extends Foo
  case class Baz(i: Int) extends Foo
  case class Bax() extends Foo
  case object Qux extends Foo
  case class Quux(b: Byte) extends Foo

  object Foo {
    implicit val schema: Tagged[Foo] = {
      val barSchema: Record[Bar] = field[String]("s").xmap(Bar(_))(_.s)
      val bazSchema: Record[Baz] = field[Int]("i").xmap(Baz(_))(_.i)
      val baxSchema: Record[Bax] = emptyRecord.xmap(_ => Bax())(_ => ())
      val quxSchema: Record[Qux.type] = emptyRecord.xmap(_ => Qux)(_ => ())
      val quuxSchema: Record[Quux] = field[Byte]("b").xmap(Quux(_))(_.b)

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
      val barSchema: Record[Bar] = field[String]("s").xmap(Bar(_))(_.s)
      val bazSchema: Record[Baz] = field[Int]("i").xmap(Baz(_))(_.i)
      val baxSchema: Record[Bax] = emptyRecord.xmap(_ => Bax())(_ => ())
      val quxSchema: Record[Qux.type] = emptyRecord.xmap(_ => Qux)(_ => ())
      val quuxSchema: Record[Quux] = field[Byte]("b").xmap(Quux(_))(_.b)

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
  val recursiveSchema: Record[Recursive] =
    lazyRecord("Rec")(
      optField("next")(recursiveSchema)
    ).xmap(Recursive(_))(_.next)
      .withDescription("Rec description")
      .withTitle("Rec title")
      .withExample(Recursive(None))

  sealed trait Expression
  object Expression {
    case class Literal(value: Int) extends Expression
    case class Add(x: Expression, y: Expression) extends Expression
  }
  val expressionSchema: JsonSchema[Expression] =
    lazySchema[Expression]("Expression") {
      intJsonSchema
        .orFallbackTo(field("x")(expressionSchema) zip field("y")(expressionSchema))
        .xmap[Expression] {
          case Left(value)   => Expression.Literal(value)
          case Right((x, y)) => Expression.Add(x, y)
        } {
          case Expression.Literal(value) => Left(value)
          case Expression.Add(x, y)      => Right((x, y))
        }
    }
      .withDescription("Expression description")
      .withTitle("Expression title")
      .withExample(Expression.Literal(1))

  case class MutualRecursiveA(b: Option[MutualRecursiveB])
  case class MutualRecursiveB(a: Option[MutualRecursiveA])
  val mutualRecursiveA: JsonSchema[MutualRecursiveA] = lazySchema("MutualRecursiveA")(
    optField("b")(mutualRecursiveB)
  ).xmap(MutualRecursiveA(_))(_.b)
  val mutualRecursiveB: JsonSchema[MutualRecursiveB] = lazySchema("MutualRecursiveB")(
    optField("a")(mutualRecursiveA)
  ).xmap(MutualRecursiveB(_))(_.a)

  sealed trait TaggedRecursive extends Product with Serializable
  case class TaggedRecursiveA(a: String, next: Option[TaggedRecursive]) extends TaggedRecursive
  case class TaggedRecursiveB(b: Int, next: Option[TaggedRecursive]) extends TaggedRecursive
  val taggedRecursiveSchema: Tagged[TaggedRecursive] =
    lazyTagged("TaggedRec") {
      val nextField = optField("next")(taggedRecursiveSchema)
      val aSchema = (field[String]("a") zip nextField)
        .xmap { case (a, next) =>
          TaggedRecursiveA(a, next)
        } { case TaggedRecursiveA(a, next) =>
          (a, next)
        }
      val bSchema = (field[Int]("b") zip nextField)
        .xmap { case (b, next) =>
          TaggedRecursiveB(b, next)
        } { case TaggedRecursiveB(b, next) =>
          (b, next)
        }

      (aSchema.tagged("A") orElseMerge bSchema.tagged("B"))
        // Put some doc inside lazyTagged, and some more outside to make sure it works both way
        .withDiscriminator("kind")
        .withDescription("TaggedRec description")
    }
      .withTitle("TaggedRec title")
      .withExample(TaggedRecursiveA("foo", None))

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

  //#numeric-constraint
  def createNumericErrorMessage[A](value: A) =
    s"$value does not satisfy the constraints: minimum:0, maximum:10, exclusiveMaximum:true, multipleOf:2"

  val constraintNumericSchema: JsonSchema[Int] =
    intWithConstraintsJsonSchema(
      NumericConstraints[Int]
        .withMinimum(Some(0))
        .withMaximum(Some(10))
        .withExclusiveMaximum(Some(true))
        .withMultipleOf(Some(2))
    )
  //#numeric-constraint

}
