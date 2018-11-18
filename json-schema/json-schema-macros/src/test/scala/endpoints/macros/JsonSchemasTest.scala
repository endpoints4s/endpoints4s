package endpoints.macros

import org.scalatest.FreeSpec

class JsonSchemasTest extends FreeSpec {

  import FakeAlgebraJsonSchemas._

  "macros derive JsonSchema for primitives" in {

    assert(genericJsonSchema[String].schema == "string")
    assert(genericJsonSchema[Int].schema == "int")
//    assert(genericJsonSchema[Long].schema == "long")
//    assert(genericJsonSchema[BigDecimal].schema == "bigdecimal")
    assert(genericJsonSchema[Float].schema == "float")
    assert(genericJsonSchema[Double].schema == "double")
//    assert(genericJsonSchema[Boolean].schema == "boolean")
  }

  "macros derive JsonSchema for case class" in {

    assert(Foo.schema.schema == "'endpoints.macros.ExampleDomain.Foo'!(bar:string,baz:int,qux:boolean?)")
  }

  "macros derive JsonSchema for sequence types" in {

    assert(listIntSchema.schema == "[int]")
    assert(seqFooSchema.schema == "['endpoints.macros.ExampleDomain.Foo'!(bar:string,baz:int,qux:boolean?)]")
  }

  "macros derive JsonSchema for records" in {

    assert(Foo.schema.schema == "'endpoints.macros.ExampleDomain.Foo'!(bar:string,baz:int,qux:boolean?)")

//    assert(QuuxA.schema.schema == "'endpoints.macros.ExampleDomain.QuuxA'!(ss:['endpoints.macros.ExampleDomain.Foo'!(bar:string,baz:int,qux:boolean?)])")
//    assert(QuuxB.schema.schema == "'endpoints.macros.ExampleDomain.QuuxB'!(i:int)")
//    assert(QuuxC.schema.schema == "'endpoints.macros.ExampleDomain.QuuxC'!(b:boolean)")
//    assert(QuuxD.schema.schema == "'endpoints.macros.ExampleDomain.QuuxD'!($)")
//    assert(QuuxE.schema.schema == "'endpoints.macros.ExampleDomain.QuuxE'!($)")
  }

  "macros derive JsonSchema for sum types" in {

//    assert(SingleCaseBase.schema.schema == "'endpoints.macros.ExampleDomain.SingleCaseBase'!('endpoints.macros.ExampleDomain.SingleCaseInst'!(foo:string)@SingleCaseInst)")
//    assert(Quux.schema.schema == "'endpoints.macros.ExampleDomain.Quux'!('endpoints.macros.ExampleDomain.QuuxA'!(ss:['endpoints.macros.ExampleDomain.Foo'!(bar:string,baz:int,qux:boolean?)])@QuuxA|'endpoints.macros.ExampleDomain.QuuxB'!(i:int)@QuuxB|'endpoints.macros.ExampleDomain.QuuxC'!(b:boolean)@QuuxC|'endpoints.macros.ExampleDomain.QuuxD'!($)@QuuxD|'endpoints.macros.ExampleDomain.QuuxE'!($)@QuuxE)")
  }

  "macros derive JsonSchema for generic types" in {

    assert(Id.schema[Int].schema == "string")
    assert(Id.schema[Float].schema == "string")
    assert(Id.schema[Foo].schema == "string")

    assert(User.schema[Int].schema == "'endpoints.macros.ExampleDomain.User'!(id:'endpoints.macros.ExampleDomain.Id'!(id:string),name:string)")
    assert(User.schema[Float].schema == "'endpoints.macros.ExampleDomain.User'!(id:'endpoints.macros.ExampleDomain.Id'!(id:string),name:string)")
    assert(User.schema[Foo].schema == "'endpoints.macros.ExampleDomain.User'!(id:'endpoints.macros.ExampleDomain.Id'!(id:string),name:string)")
  }
}
