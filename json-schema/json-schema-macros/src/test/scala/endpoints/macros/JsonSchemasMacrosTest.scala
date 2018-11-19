package endpoints.macros

import org.scalatest.FreeSpec

class JsonSchemasMacrosTest extends FreeSpec {

  import TestJsonSchemas._
  import TestJsonSchemas.JsonSchemas._

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

    assert(quuxASchema == "'endpoints.macros.ExampleDomain.QuuxA'!(ss:['endpoints.macros.ExampleDomain.Foo'!(bar:string,baz:int,qux:boolean?)])")
    assert(quuxBSchema == "'endpoints.macros.ExampleDomain.QuuxB'!(i:int)")
    assert(quuxCSchema == "'endpoints.macros.ExampleDomain.QuuxC'!(b:boolean)")
    assert(quuxDSchema == "'endpoints.macros.ExampleDomain.QuuxD'!($)")
    assert(quuxESchema == "'endpoints.macros.ExampleDomain.QuuxE'!($)")
  }

  "macros derive JsonSchema for sum types" in {

    assert(singleCaseBaseSchema == "'endpoints.macros.ExampleDomain.SingleCaseBase'!('endpoints.macros.ExampleDomain.SingleCaseInst'!(foo:string)@SingleCaseInst)")
    assert(quuxSchema == "'endpoints.macros.ExampleDomain.Quux'!('endpoints.macros.ExampleDomain.QuuxA'!(ss:['endpoints.macros.ExampleDomain.Foo'!(bar:string,baz:int,qux:boolean?)])@QuuxA|'endpoints.macros.ExampleDomain.QuuxB'!(i:int)@QuuxB|'endpoints.macros.ExampleDomain.QuuxC'!(b:boolean)@QuuxC|'endpoints.macros.ExampleDomain.QuuxD'!($)@QuuxD|'endpoints.macros.ExampleDomain.QuuxE'!($)@QuuxE)")
  }

  "macros derive JsonSchema for generic types" in {

    assert(idSchema[Int] == "string")
    assert(idSchema[Float] == "string")
    assert(idSchema[Foo] == "string")

    assert(userSchema[Int] == "'endpoints.macros.ExampleDomain.User'!(id:string,name:string)")
    assert(userSchema[Float] == "'endpoints.macros.ExampleDomain.User'!(id:string,name:string)")
    assert(userSchema[Foo] == "'endpoints.macros.ExampleDomain.User'!(id:string,name:string)")
  }
}
