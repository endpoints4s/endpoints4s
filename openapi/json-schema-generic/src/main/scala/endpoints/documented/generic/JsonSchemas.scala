package endpoints
package documented
package generic

import endpoints.algebra.JsonSchemas
import shapeless.labelled.{FieldType, field => shapelessField}
import shapeless.ops.hlist.Tupler
import shapeless.{:+:, ::, CNil, Coproduct, Generic, HList, HNil, Inl, Inr, LabelledGeneric, Witness}

import scala.language.implicitConversions

/**
  * Enriches [[JsonSchemas]] with two kinds of operations:
  *
  * - `genericJsonSchema[A]` derives the `JsonSchema` of an algebraic
  *   data type `A`;
  * - `(field1 :×: field2 :×: …).as[A]` builds a tuple of `Record`s and maps
  *   it to a case class `A`
  *
  * The data type description derivation is based on the underlying
  * field and constructor names.
  *
  * For instance, consider the following program that derives the description
  * of a case class:
  *
  * {{{
  *   case class User(name: String, age: Int)
  *   object User {
  *     implicit val schema: JsonSchema[User] = genericJsonSchema[User]
  *   }
  * }}}
  *
  * It is equivalent to the following:
  *
  * {{{
  *   case class User(name: String, age: Int)
  *   object User {
  *     implicit val schema: JsonSchema[User] = (
  *       field[String]("name") zip
  *       field[Int]("age")
  *     ).invmap((User.apply _).tupled)(Function.unlift(User.unapply))
  *   }
  * }}}
  *
  */
trait JsonSchemas extends JsonSchemas {

  trait GenericJsonSchema[A] {
    def jsonSchema: JsonSchema[A]
  }

  object GenericJsonSchema extends GenericJsonSchemaLowPriority {

    implicit def singletonRecord[L <: Symbol, A](implicit
      labelSingleton: Witness.Aux[L],
      jsonSchemaSingleton: JsonSchema[A]
    ): GenericRecord[FieldType[L, A] :: HNil] =
      new GenericRecord[FieldType[L, A] :: HNil] {
        def jsonSchema: Record[FieldType[L, A] :: HNil] =
          field(labelSingleton.value.name)(jsonSchemaSingleton)
            .invmap[FieldType[L, A] :: HNil](a => shapelessField[L](a) :: HNil)(_.head)
      }

    implicit def singletonCoproduct[L <: Symbol, A](implicit
      labelSingleton: Witness.Aux[L],
      recordA: GenericRecord[A]
    ): GenericTagged[FieldType[L, A] :+: CNil] =
      new GenericTagged[FieldType[L, A] :+: CNil] {
        def jsonSchema: Tagged[FieldType[L, A] :+: CNil] =
          recordA.jsonSchema.tagged(labelSingleton.value.name).invmap[FieldType[L, A] :+: CNil] {
            a => Inl(shapelessField[L](a))
          } {
            case Inl(a) => a
            case Inr(_) => sys.error("Unreachable code")
          }
      }

  }

  trait GenericJsonSchemaLowPriority extends GenericJsonSchemaLowLowPriority {

    implicit def consRecord[L <: Symbol, H, T <: HList](implicit
      labelHead: Witness.Aux[L],
      jsonSchemaHead: JsonSchema[H],
      jsonSchemaTail: GenericRecord[T]
    ): GenericRecord[FieldType[L, H] :: T] =
      new GenericRecord[FieldType[L, H] :: T] {
        def jsonSchema: Record[FieldType[L, H] :: T] =
          (field(labelHead.value.name)(jsonSchemaHead) zip jsonSchemaTail.jsonSchema)
            .invmap[FieldType[L, H] :: T] { case (h, t) => shapelessField[L](h) :: t }(ht => (ht.head, ht.tail))
      }

    implicit def consCoproduct[L <: Symbol, H, T <: Coproduct](implicit
      labelHead: Witness.Aux[L],
      recordHead: GenericRecord[H],
      taggedTail: GenericTagged[T]
    ): GenericTagged[FieldType[L, H] :+: T] =
      new GenericTagged[FieldType[L, H] :+: T] {
        def jsonSchema: Tagged[FieldType[L, H] :+: T] = {
          val taggedHead = recordHead.jsonSchema.tagged(labelHead.value.name)
          taggedHead.orElse(taggedTail.jsonSchema).invmap[FieldType[L, H] :+: T] {
            case Left(h)  => Inl(shapelessField[L](h))
            case Right(t) => Inr(t)
          } {
            case Inl(h) => Left(h)
            case Inr(t) => Right(t)
          }
        }
      }

  }

  trait GenericJsonSchemaLowLowPriority {

    trait GenericRecord[A] extends GenericJsonSchema[A] {
      def jsonSchema: Record[A]
    }

    trait GenericTagged[A] extends GenericJsonSchema[A] {
      def jsonSchema: Tagged[A]
    }

    implicit def recordGeneric[A, R](implicit
      gen: LabelledGeneric.Aux[A, R],
      record: GenericRecord[R]
    ): GenericRecord[A] =
      new GenericRecord[A] {
        def jsonSchema: Record[A] = record.jsonSchema.invmap[A](gen.from)(gen.to)
      }

    implicit def taggedGeneric[A, R](implicit
      gen: LabelledGeneric.Aux[A, R],
      tagged: GenericTagged[R]
    ): GenericTagged[A] =
      new GenericTagged[A] {
        def jsonSchema: Tagged[A] = tagged.jsonSchema.invmap[A](gen.from)(gen.to)
      }
  }

  /** @return a `JsonSchema[A]` obtained from an implicitly derived `GenericJsonSchema[A]`
    *
    * In a sense, this operation asks shapeless to compute a ''type level'' description
    * of a data type (based on HLists and Coproducts) and turns it into a ''term level''
    * description of the data type (based on the `JsonSchemas` algebra interface)
    */
  def genericJsonSchema[A](implicit genJsonSchema: GenericJsonSchema[A]): JsonSchema[A] = genJsonSchema.jsonSchema

  final class RecordGenericOps[L <: HList](record: Record[L]) {

    def :*: [H](recordHead: Record[H]): RecordGenericOps[H :: L] =
      new RecordGenericOps(
        (recordHead zip record).invmap { case (h, l) => h :: l }(hl => (hl.head, hl.tail))
      )

    def :×: [H](recordHead: Record[H]): RecordGenericOps[H :: L] = recordHead :*: this

    def as[A](implicit gen: Generic.Aux[A, L]): Record[A] = record.invmap(gen.from)(gen.to)

    def tupled[T](implicit
      tupler: Tupler.Aux[L, T],
      gen: Generic.Aux[T, L]
    ): Record[T] = as[T]

  }

  implicit def toRecordGenericOps[A](record: Record[A]): RecordGenericOps[A :: HNil] =
    new RecordGenericOps[A :: HNil](record.invmap(_ :: HNil)(_.head))

}