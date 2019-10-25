package endpoints
package generic

import shapeless.labelled.{FieldType, field => shapelessField}
import shapeless.{:+:, ::, Annotations, CNil, Coproduct, Generic, HList, HNil, Inl, Inr, LabelledGeneric, Witness}

import scala.language.implicitConversions
import scala.language.higherKinds
import scala.reflect.ClassTag

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
  *     ).xmap((User.apply _).tupled)(Function.unlift(User.unapply))
  *   }
  * }}}
  *
  */
trait JsonSchemas extends algebra.JsonSchemas {

  trait GenericJsonSchema[A] {
    def jsonSchema: JsonSchema[A]
  }

  object GenericJsonSchema extends GenericJsonSchemaLowPriority {

    implicit def emptyRecordCase: DocumentedGenericRecord[HNil, HNil] =
      (docs: HNil) => emptyRecord.xmap[HNil](_ => HNil)(_ => ())

    implicit def singletonCoproduct[L <: Symbol, A](implicit
      labelSingleton: Witness.Aux[L],
      recordA: GenericRecord[A]
    ): GenericTagged[FieldType[L, A] :+: CNil] =
      new GenericTagged[FieldType[L, A] :+: CNil](
        jsonSchema =
          recordA.jsonSchema.tagged(labelSingleton.value.name).xmap[FieldType[L, A] :+: CNil] {
            a => Inl(shapelessField[L](a))
          } {
            case Inl(a) => a
            case Inr(_) => sys.error("Unreachable code")
          }
    )

  }

  trait GenericJsonSchemaLowPriority extends GenericJsonSchemaLowLowPriority {

    implicit def consRecord[L <: Symbol, H, T <: HList, DH <: Option[docs], DT <: HList](implicit
      labelHead: Witness.Aux[L],
      jsonSchemaHead: JsonSchema[H],
      jsonSchemaTail: DocumentedGenericRecord[T, DT]
    ): DocumentedGenericRecord[FieldType[L, H] :: T, DH :: DT] =
      new DocumentedGenericRecord[FieldType[L, H] :: T, DH :: DT] {
        def record(docs: DH :: DT) =
          (field(labelHead.value.name, docs.head.map(_.text))(jsonSchemaHead) zip jsonSchemaTail.record(docs.tail))
            .xmap[FieldType[L, H] :: T] { case (h, t) => shapelessField[L](h) :: t }(ht => (ht.head, ht.tail))
      }

    implicit def consOptRecord[L <: Symbol, H, T <: HList, DH <: Option[docs], DT <: HList](implicit
      labelHead: Witness.Aux[L],
      jsonSchemaHead: JsonSchema[H],
      jsonSchemaTail: DocumentedGenericRecord[T, DT]
    ): DocumentedGenericRecord[FieldType[L, Option[H]] :: T, DH :: DT] =
      new DocumentedGenericRecord[FieldType[L, Option[H]] :: T, DH :: DT] {
        def record(docs: DH :: DT) =
          (optField(labelHead.value.name, docs.head.map(_.text))(jsonSchemaHead) zip jsonSchemaTail.record(docs.tail))
            .xmap[FieldType[L, Option[H]] :: T] { case (h, t) => shapelessField[L](h) :: t }(ht => (ht.head, ht.tail))
      }

    implicit def consCoproduct[L <: Symbol, H, T <: Coproduct](implicit
      labelHead: Witness.Aux[L],
      recordHead: GenericRecord[H],
      taggedTail: GenericTagged[T]
    ): GenericTagged[FieldType[L, H] :+: T] =
      new GenericTagged[FieldType[L, H] :+: T](
        jsonSchema = {
          val taggedHead = recordHead.jsonSchema.tagged(labelHead.value.name)
          taggedHead.orElse(taggedTail.jsonSchema).xmap[FieldType[L, H] :+: T] {
            case Left(h)  => Inl(shapelessField[L](h))
            case Right(t) => Inr(t)
          } {
            case Inl(h) => Left(h)
            case Inr(t) => Right(t)
          }
        }
      )

  }

  trait GenericJsonSchemaLowLowPriority {

    class GenericRecord[A](val jsonSchema: Record[A]) extends GenericJsonSchema[A]

    class GenericTagged[A](val jsonSchema: Tagged[A]) extends GenericJsonSchema[A]

    trait DocumentedGenericRecord[A, D <: HList] {
      def record(docs: D): Record[A]
    }

    implicit def recordGeneric[A, R, D <: HList](implicit
      gen: LabelledGeneric.Aux[A, R],
      docAnns: Annotations.Aux[docs, A, D],
      record: DocumentedGenericRecord[R, D],
      ct: ClassTag[A]
    ): GenericRecord[A] =
      new GenericRecord[A](record.record(docAnns()).xmap[A](gen.from)(gen.to).named(classTagToSchemaName(ct)))

    implicit def taggedGeneric[A, R](implicit
      gen: LabelledGeneric.Aux[A, R],
      tagged: GenericTagged[R],
      ct: ClassTag[A]
    ): GenericTagged[A] =
      new GenericTagged[A](tagged.jsonSchema.xmap[A](gen.from)(gen.to).named(classTagToSchemaName(ct)))

  }

  /**
    * Compute a schema name (used for documentation) based on a `ClassTag`.
    * The provided implementation uses the fully qualified name of the class.
    * You can override this method to use a custom logic.
    */
  def classTagToSchemaName(ct: ClassTag[_]): String = {
    val jvmName = ct.runtimeClass.getName
    // name fix for case objects
    val name = if(jvmName.nonEmpty && jvmName.last == '$') jvmName.init else jvmName
    name.replace('$','.')
  }

  /** @return a `JsonSchema[A]` obtained from an implicitly derived `GenericJsonSchema[A]`
    *
    * In a sense, this operation asks shapeless to compute a ''type level'' description
    * of a data type (based on HLists and Coproducts) and turns it into a ''term level''
    * description of the data type (based on the `JsonSchemas` algebra interface)
    *
    * This operation is calculating a name for the schema based on classTag.runtimeClass.getName
    * This could result in non unique values and mess with documentation
    */
  def genericJsonSchema[A](implicit genJsonSchema: GenericJsonSchema[A]): JsonSchema[A] =
    genJsonSchema.jsonSchema

  /** @return a `Record[A]` obtained from an implicitly derived `GenericRecord[A]`
    *
    * This operation is calculating a name for the schema based on classTag.runtimeClass.getName
    * This could result in non unique values and mess with documentation
    */
  def genericRecord[A](implicit genRecord: GenericJsonSchema.GenericRecord[A]): Record[A] =
    genRecord.jsonSchema

  /** @return a `Tagged[A]` obtained from an implicitly derived `GenericTagged[A]`
    *
    * This operation is calculating a name for the schema based on classTag.runtimeClass.getName
    * This could result in non unique values and mess with documentation
    */
  def genericTagged[A](implicit genTagged: GenericJsonSchema.GenericTagged[A]): Tagged[A] =
    genTagged.jsonSchema

  final class RecordGenericOps[L <: HList](record: Record[L]) {

    def :*: [H](recordHead: Record[H]): RecordGenericOps[H :: L] =
      new RecordGenericOps(
        (recordHead zip record).xmap { case (h, l) => h :: l }(hl => (hl.head, hl.tail))
      )

    def :×: [H](recordHead: Record[H]): RecordGenericOps[H :: L] = recordHead :*: this

    def as[A](implicit gen: Generic.Aux[A, L]): Record[A] = record.xmap(gen.from)(gen.to)

    def tupled[T](implicit gen: Generic.Aux[T, L]): Record[T] = as[T]

  }

  implicit def toRecordGenericOps[A](record: Record[A]): RecordGenericOps[A :: HNil] =
    new RecordGenericOps[A :: HNil](record.xmap(_ :: HNil)(_.head))

}
