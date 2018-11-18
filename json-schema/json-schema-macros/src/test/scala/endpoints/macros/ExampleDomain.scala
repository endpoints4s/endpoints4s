package endpoints.macros


trait ExampleDomain extends JsonSchemas {

  case class Foo(bar: String, baz: Int, qux: Option[Boolean])

  implicit val fooSchema: JsonSchema[Foo] = genericJsonSchema[Foo]

  // compiler can't pick implicit schema from companion object, but it does when
  // it's defined outside! weird...

//    object Foo {
//      implicit val schema: JsonSchema[Foo] = genericJsonSchema[Foo]
//    }


  sealed trait Quux

  case class QuuxA(ss: List[Foo]) extends Quux

  object QuuxA {
    implicit val schema: JsonSchema[QuuxA] = genericJsonSchema[QuuxA]
  }

  case class QuuxB(i: Int) extends Quux

  object QuuxB {
    implicit val schema: JsonSchema[QuuxB] = genericJsonSchema[QuuxB]
  }

  case class QuuxC(b: Boolean) extends Quux

  object QuuxC {
    implicit val schema: JsonSchema[QuuxC] = genericJsonSchema[QuuxC]
  }

  case class QuuxD() extends Quux

  object QuuxD {
    implicit val schema: JsonSchema[QuuxD] = genericJsonSchema[QuuxD]
  }

  case object QuuxE extends Quux {
    implicit val schema: JsonSchema[QuuxE.type] = genericJsonSchema[QuuxE.type]
  }

  object Quux {
    implicit val schema: JsonSchema[Quux] = genericJsonSchema[Quux]
  }

  sealed trait SingleCaseBase

  case class SingleCaseInst(foo: String) extends SingleCaseBase
  object SingleCaseInst {
    implicit val schema: JsonSchema[SingleCaseInst] = genericJsonSchema[SingleCaseInst]
  }

  object SingleCaseBase {
    implicit val schema: JsonSchema[SingleCaseBase] = genericJsonSchema[SingleCaseBase]
  }

  val listIntSchema: JsonSchema[List[Int]] = genericJsonSchema[List[Int]]

  val seqFooSchema: JsonSchema[List[Foo]] = genericJsonSchema[List[Foo]]

  case class Id[T](id: String)

  object Id {
    implicit def schema[T]: JsonSchema[Id[T]] =
      stringJsonSchema.invmap(Id.apply[T] _)(_.id)
  }

  case class User[T](id: Id[T], name: String)

  object User {
    implicit def schema[T]: JsonSchema[User[T]] = genericJsonSchema[User[T]]
  }

}
