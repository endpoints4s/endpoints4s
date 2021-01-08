package endpoints4s
package generic

import scala.collection.compat._
import org.scalatest.freespec.AnyFreeSpec

class JsonSchemasTest extends AnyFreeSpec {

  trait GenericSchemas extends JsonSchemas {

    case class Foo(bar: String, baz: Int, qux: Option[Boolean])

    object Foo {
      val schema: JsonSchema[Foo] = genericJsonSchema[Foo]
    }

    sealed trait Quux
    case class QuuxA(ss: List[String]) extends Quux
    case class QuuxB(id: (Int, Double)) extends Quux
    case class QuuxC(b: Boolean) extends Quux
    case class QuuxD() extends Quux
    case object QuuxE extends Quux

    object Quux {
      implicit val schema: JsonSchema[Quux] = genericJsonSchema[Quux]
    }

    @discriminator("$type")
    @name("DocResource")
    @docs("traitDoc")
    @title("Doc Resource")
    sealed trait Doc
    @docs("recordDocA")
    case class DocA(@docs("fieldDocI") i: Int) extends Doc
    @unnamed()
    case class DocB(
        a: String,
        @docs("fieldDocB") b: Boolean,
        @docs("fieldDocSS") ss: List[String]
    ) extends Doc
    @name("DocC")
    @docs("recordDocC")
    case object DocC extends Doc

    object Doc {
      val schema: JsonSchema[Doc] = genericJsonSchema[Doc]
    }

    val tupledSchema = field[Int]("x") zip field[Int]("y")
    val tupledSchema2: JsonSchema[(Int, Int)] = tupledSchema

    case class Point2(x: Int, y: Int)
    val schema3: JsonSchema[Point2] = tupledSchema2.as[Point2]

    case class Point3(x: Int, y: Int, z: Int)
    val schemaPoint3 =
      (field[Int]("x") zip field[Int]("y") zip field[Int]("z")).as[Point3]

    //#custom-schema
    sealed trait Shape
    case class Circle(radius: Double) extends Shape
    case class Rectangle(width: Double, height: Double) extends Shape

    implicit val shapeSchema: JsonSchema[Shape] = {
      // For some reason, our JSON representation uses a `diameter`
      // rather than a `radius`
      val circleSchema: Record[Circle] =
        field[Double]("diameter")
          .xmap(diameter => Circle(diameter / 2))(circle => circle.radius * 2)
      implicit val circleGenericRecord: GenericJsonSchema.GenericRecord[Circle] =
        new GenericJsonSchema.GenericRecord(circleSchema)
      // The generic schema for `Shape` will synthesize the schema for `Rectangle`,
      // but it will use the implicitly provided `GenericRecord[Circle]` for `Circle.
      genericJsonSchema[Shape]
    }
    //#custom-schema

    case class CaseClassWithDefaultValue(x: Int, s: String = "hello")
    val caseClassWithDefaultValueSchema: JsonSchema[CaseClassWithDefaultValue] = genericJsonSchema

  }

  object FakeAlgebraJsonSchemas
      extends GenericSchemas
      with endpoints4s.algebra.JsonSchemas
      with FakeTuplesSchemas {

    type Record[+A] = String
    type Tagged[+A] = String
    type Enum[+A] = String

    def enumeration[A](values: Seq[A])(tpe: JsonSchema[A]): String =
      s"$tpe"

    def namedRecord[A](schema: Record[A], name: String): Record[A] =
      s"'$name'!($schema)"
    def namedTagged[A](schema: Tagged[A], name: String): Tagged[A] =
      s"'$name'!($schema)"
    def namedEnum[A](schema: Enum[A], name: String): Enum[A] =
      s"'$name'!($schema)"

    def lazyRecord[A](schema: => Record[A], name: String): JsonSchema[A] =
      s"=>'$name'!($schema)"
    def lazyTagged[A](schema: => Tagged[A], name: String): JsonSchema[A] =
      s"=>'$name'!($schema)"

    def emptyRecord: String =
      "%"

    def field[A](name: String, docs: Option[String])(implicit
        tpe: String
    ): String =
      s"$name:$tpe${docs.fold("")(doc => s"{$doc}")}"

    def optField[A](name: String, docs: Option[String])(implicit
        tpe: String
    ): String =
      s"$name:$tpe?${docs.fold("")(doc => s"{$doc}")}"

    def taggedRecord[A](recordA: String, tag: String): String =
      s"$recordA@$tag"

    def withDiscriminatorTagged[A](
        tagged: Tagged[A],
        discriminatorName: String
    ): Tagged[A] =
      s"$tagged#$discriminatorName"

    def choiceTagged[A, B](taggedA: String, taggedB: String): String =
      s"$taggedA|$taggedB"

    def zipRecords[A, B](recordA: String, recordB: String)(implicit
        t: Tupler[A, B]
    ): String =
      s"$recordA,$recordB"

    def withExampleRecord[A](
        schema: Record[A],
        example: A
    ): Record[A] = schema

    def withExampleTagged[A](
        schema: Tagged[A],
        example: A
    ): Tagged[A] = schema

    def withExampleEnum[A](
        schema: Enum[A],
        example: A
    ): Enum[A] = schema

    def withExampleJsonSchema[A](
        schema: JsonSchema[A],
        example: A
    ): JsonSchema[A] =
      schema

    def withDescriptionRecord[A](
        schema: Record[A],
        description: String
    ): String = s"$schema{$description}"

    def withDescriptionTagged[A](
        schema: Tagged[A],
        description: String
    ): String = s"$schema{$description}"

    def withDescriptionEnum[A](
        schema: Enum[A],
        description: String
    ): String = s"$schema{$description}"

    def withDescriptionJsonSchema[A](
        schema: JsonSchema[A],
        description: String
    ): String = s"$schema{$description}"

    def withTitleRecord[A](schema: Record[A], title: String): Record[A] =
      s"[[$title]]$schema"

    def withTitleTagged[A](schema: Tagged[A], title: String): Tagged[A] =
      s"[[$title]]$schema"

    def withTitleEnum[A](schema: Enum[A], title: String): Enum[A] =
      s"[[$title]]$schema"

    def withTitleJsonSchema[A](
        schema: JsonSchema[A],
        title: String
    ): JsonSchema[A] = schema

    def orFallbackToJsonSchema[A, B](
        schemaA: JsonSchema[A],
        schemaB: JsonSchema[B]
    ): JsonSchema[Either[A, B]] =
      s"$schemaA|$schemaB"

    def jsonSchemaPartialInvFunctor: PartialInvariantFunctor[JsonSchema] =
      new PartialInvariantFunctor[JsonSchema] {
        def xmapPartial[A, B](
            fa: String,
            f: A => Validated[B],
            g: B => A
        ): String = fa
        override def xmap[A, B](fa: String, f: A => B, g: B => A): String = fa
      }
    def recordPartialInvFunctor: PartialInvariantFunctor[Record] =
      new PartialInvariantFunctor[Record] {
        def xmapPartial[A, B](
            fa: String,
            f: A => Validated[B],
            g: B => A
        ): String = fa
        override def xmap[A, B](fa: String, f: A => B, g: B => A): String = fa
      }
    def taggedPartialInvFunctor: PartialInvariantFunctor[Tagged] =
      new PartialInvariantFunctor[Tagged] {
        def xmapPartial[A, B](
            fa: String,
            f: A => Validated[B],
            g: B => A
        ): String = fa
        override def xmap[A, B](fa: String, f: A => B, g: B => A): String = fa
      }

    def stringJsonSchema(format: Option[String]): String = "string"

    lazy val intJsonSchema: String = "integer"

    lazy val longJsonSchema: String = "integer"

    lazy val bigdecimalJsonSchema: String = "number"

    lazy val floatJsonSchema: String = "number"

    lazy val doubleJsonSchema: String = "number"

    lazy val booleanJsonSchema: String = "boolean"

    lazy val byteJsonSchema: String = "byte"

    def arrayJsonSchema[C[X] <: Iterable[X], A](implicit
        jsonSchema: String,
        factory: Factory[A, C[A]]
    ): String = s"[$jsonSchema]"

    def mapJsonSchema[A](implicit
        jsonSchema: String
    ): String = s"{$jsonSchema}"
  }

  val ns = "endpoints4s.generic.JsonSchemasTest.GenericSchemas"

  "case class" in {
    val expectedSchema = s"'$ns.Foo'!(bar:string,baz:integer,qux:boolean?,%)"
    assert(FakeAlgebraJsonSchemas.Foo.schema == expectedSchema)
  }

  "sealed trait" in {
    val expectedSchema = s"'$ns.Quux'!(${List(
      s"'$ns.QuuxA'!(ss:[string],%)@QuuxA",
      s"'$ns.QuuxB'!(id:[integer, number],%)@QuuxB",
      s"'$ns.QuuxC'!(b:boolean,%)@QuuxC",
      s"'$ns.QuuxD'!(%)@QuuxD",
      s"'$ns.QuuxE'!(%)@QuuxE"
    ).mkString("|")})#type"
    assert(FakeAlgebraJsonSchemas.Quux.schema == expectedSchema)
  }

  "documentations" in {
    val expectedSchema = s"[[Doc Resource]]'DocResource'!(${List(
      s"'$ns.DocA'!(i:integer{fieldDocI},%){recordDocA}@DocA",
      s"a:string,b:boolean{fieldDocB},ss:[string]{fieldDocSS},%@DocB",
      s"'DocC'!(%){recordDocC}@DocC"
    ).mkString("|")})#$$type{traitDoc}"
    assert(FakeAlgebraJsonSchemas.Doc.schema == expectedSchema)
  }

  "custom schema" in {
    val expectedSchema =
      s"'$ns.Shape'!(diameter:number@Circle|'$ns.Rectangle'!(width:number,height:number,%)@Rectangle)#type"
    assert(FakeAlgebraJsonSchemas.shapeSchema == expectedSchema)
  }

  "case class parameter default values" in {
    // Note that `s:string?` is optional
    val expectedSchema = s"'$ns.CaseClassWithDefaultValue'!(x:integer,s:string?,%)"
    assert(FakeAlgebraJsonSchemas.caseClassWithDefaultValueSchema == expectedSchema)
  }

}
