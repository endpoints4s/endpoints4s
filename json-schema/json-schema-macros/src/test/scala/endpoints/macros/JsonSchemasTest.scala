package endpoints.macros

import org.scalatest.FreeSpec

class JsonSchemasTest extends FreeSpec {

  import FakeAlgebraJsonSchemas._

  "macros derive JsonSchema for primitives" in {

    assert(genericJsonSchema[String] == "string")
    assert(genericJsonSchema[Int] == "int")
    assert(genericJsonSchema[Long] == "long")
    assert(genericJsonSchema[BigDecimal] == "bigdecimal")
    assert(genericJsonSchema[Float] == "float")
    assert(genericJsonSchema[Double] == "double")
    assert(genericJsonSchema[Boolean] == "boolean")
  }

  "macros derive JsonSchema for case class" in {

    assert(fooSchema == "'endpoints.macros.ExampleDomain.Foo'!(bar:string,baz:int,qux:boolean?)")
  }

  "macros derive JsonSchema for sequence types" in {

    assert(listIntSchema == "[int]")
    assert(seqFooSchema == "['endpoints.macros.ExampleDomain.Foo'!(bar:string,baz:int,qux:boolean?)]")
  }

  "macros derive JsonSchema for records" in {

    assert(fooSchema == "'endpoints.macros.ExampleDomain.Foo'!(bar:string,baz:int,qux:boolean?)")

    assert(QuuxA.schema == "'endpoints.macros.ExampleDomain.QuuxA'!(ss:['endpoints.macros.ExampleDomain.Foo'!(bar:string,baz:int,qux:boolean?)])")
    assert(QuuxB.schema == "'endpoints.macros.ExampleDomain.QuuxB'!(i:int)")
    assert(QuuxC.schema == "'endpoints.macros.ExampleDomain.QuuxC'!(b:boolean)")
    assert(QuuxD.schema == "'endpoints.macros.ExampleDomain.QuuxD'!($)")
    assert(QuuxE.schema == "'endpoints.macros.ExampleDomain.QuuxE'!($)")
  }

  "macros derive JsonSchema for sum types" in {

    assert(SingleCaseBase.schema == "'endpoints.macros.ExampleDomain.SingleCaseBase'!('endpoints.macros.ExampleDomain.SingleCaseInst'!(foo:string)@SingleCaseInst)")
    assert(Quux.schema == "'endpoints.macros.ExampleDomain.Quux'!('endpoints.macros.ExampleDomain.QuuxA'!(ss:['endpoints.macros.ExampleDomain.Foo'!(bar:string,baz:int,qux:boolean?)])@QuuxA|'endpoints.macros.ExampleDomain.QuuxB'!(i:int)@QuuxB|'endpoints.macros.ExampleDomain.QuuxC'!(b:boolean)@QuuxC|'endpoints.macros.ExampleDomain.QuuxD'!($)@QuuxD|'endpoints.macros.ExampleDomain.QuuxE'!($)@QuuxE)")
  }

  "macros derive JsonSchema for generic types" in {

    assert(Id.schema[Int] == "string")
    assert(Id.schema[Float] == "string")
    assert(Id.schema[Foo] == "string")

    assert(User.schema[Int] == "'endpoints.macros.ExampleDomain.User'!(id:'endpoints.macros.ExampleDomain.Id'!(id:string),name:string)")
    assert(User.schema[Float] == "'endpoints.macros.ExampleDomain.User'!(id:'endpoints.macros.ExampleDomain.Id'!(id:string),name:string)")
    assert(User.schema[Foo] == "'endpoints.macros.ExampleDomain.User'!(id:'endpoints.macros.ExampleDomain.Id'!(id:string),name:string)")
  }
}
