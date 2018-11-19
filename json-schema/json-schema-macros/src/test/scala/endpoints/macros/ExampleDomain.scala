package endpoints.macros


trait ExampleDomain extends JsonSchemas {

  case class Foo(bar: String, baz: Int, qux: Option[Boolean])

  sealed trait Quux
  case class QuuxA(ss: List[Foo]) extends Quux
  case class QuuxB(i: Int) extends Quux
  case class QuuxC(b: Boolean) extends Quux
  case class QuuxD() extends Quux
  case object QuuxE extends Quux

  sealed trait SingleCaseBase
  case class SingleCaseInst(foo: String) extends SingleCaseBase

  case class Id[T](id: String)
  case class User[T](id: Id[T], name: String)

  object JsonSchemas {
    implicit val fooSchema: JsonSchema[Foo] = genericJsonSchema[Foo]

    val listIntSchema: JsonSchema[List[Int]] = genericJsonSchema[List[Int]]
    val seqFooSchema: JsonSchema[List[Foo]] = genericJsonSchema[List[Foo]]

    implicit val quuxASchema: JsonSchema[QuuxA] = genericJsonSchema[QuuxA]
    implicit val quuxBSchema: JsonSchema[QuuxB] = genericJsonSchema[QuuxB]
    implicit val quuxCSchema: JsonSchema[QuuxC] = genericJsonSchema[QuuxC]
    implicit val quuxDSchema: JsonSchema[QuuxD] = genericJsonSchema[QuuxD]
    implicit val quuxESchema: JsonSchema[QuuxE.type] = genericJsonSchema[QuuxE.type]
    implicit val quuxSchema: JsonSchema[Quux] = genericJsonSchema[Quux]

    implicit val singleCaseInstSchema: JsonSchema[SingleCaseInst] = genericJsonSchema[SingleCaseInst]
    implicit val singleCaseBaseSchema: JsonSchema[SingleCaseBase] = genericJsonSchema[SingleCaseBase]

    implicit def idSchema[T]: JsonSchema[Id[T]] =
      stringJsonSchema.invmap(Id.apply[T])(_.id)

    implicit def userSchema[T]: JsonSchema[User[T]] =
      genericJsonSchema[User[T]]
  }
}
