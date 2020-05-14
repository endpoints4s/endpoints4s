package endpoints
package generic

import shapeless.labelled.{FieldType, field => shapelessField}
import shapeless.ops.hlist.Tupler
import shapeless.{
  :+:,
  ::,
  Annotation,
  Annotations,
  CNil,
  Coproduct,
  Generic,
  HList,
  HNil,
  Inl,
  Inr,
  LabelledGeneric,
  Witness
}

import scala.annotation.{implicitNotFound, nowarn}
import scala.reflect.ClassTag

/**
  * Enriches [[JsonSchemas]] with two kinds of operations:
  *
  *   - `genericJsonSchema[A]` derives the `JsonSchema` of an algebraic
  *     data type `A`;
  *   - `(field1 :×: field2 :×: …).as[A]` builds a tuple of `Record`s and maps
  *     it to a case class `A`
  *
  * For instance, consider the following program that derives the JSON schema
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

  @implicitNotFound(
    "Unable to derive an instance of JsonSchema[${A}].\n" +
      "The type ${A} must be a sealed trait or a case class.\n" +
      "If it is a sealed trait, make sure it is only extended by case classes and that a " +
      "JsonSchema can be derived for each of these case classes (see hereafter).\n" +
      "If it is a case class, make sure each field of the case class has an implicit JsonSchema.\n" +
      "You can check that a JsonSchema is available for a type T by compiling the expression " +
      "`implicitly[JsonSchema[T]]`."
  )
  trait GenericJsonSchema[A] {
    def jsonSchema: JsonSchema[A]
  }

  object GenericJsonSchema
      extends GenericJsonSchemaLowPriority
      with GenericDiscriminatorNames
      with GenericSchemaNames
      with GenericDescriptions
      with GenericTitles {

    implicit def emptyRecordCase: DocumentedGenericRecord[HNil, HNil] =
      (_: HNil) => emptyRecord.xmap[HNil](_ => HNil)(_ => ())

    implicit def singletonCoproduct[L <: Symbol, A](
        implicit
        labelSingleton: Witness.Aux[L],
        recordA: GenericRecord[A]
    ): GenericTagged[FieldType[L, A] :+: CNil] =
      new GenericTagged[FieldType[L, A] :+: CNil](
        jsonSchema = recordA.jsonSchema
          .tagged(labelSingleton.value.name)
          .xmap[FieldType[L, A] :+: CNil] { a => Inl(shapelessField[L](a)) } {
            case Inl(a) => a
            case Inr(_) => sys.error("Unreachable code")
          }
      )

  }

  trait GenericJsonSchemaLowPriority extends GenericJsonSchemaLowLowPriority {
    this: GenericJsonSchema.type =>

    implicit def consRecord[L <: Symbol, H, T <: HList, DH <: Option[docs], DT <: HList](
        implicit
        labelHead: Witness.Aux[L],
        jsonSchemaHead: JsonSchema[H],
        jsonSchemaTail: DocumentedGenericRecord[T, DT]
    ): DocumentedGenericRecord[FieldType[L, H] :: T, DH :: DT] =
      new DocumentedGenericRecord[FieldType[L, H] :: T, DH :: DT] {
        def record(docs: DH :: DT) =
          (field(labelHead.value.name, docs.head.map(_.text))(jsonSchemaHead) zip jsonSchemaTail
            .record(docs.tail))
            .xmap[FieldType[L, H] :: T] {
              case (h, t) => shapelessField[L](h) :: t
            }(ht => (ht.head, ht.tail))
      }

    implicit def consOptRecord[L <: Symbol, H, T <: HList, DH <: Option[docs], DT <: HList](
        implicit
        labelHead: Witness.Aux[L],
        jsonSchemaHead: JsonSchema[H],
        jsonSchemaTail: DocumentedGenericRecord[T, DT]
    ): DocumentedGenericRecord[FieldType[L, Option[H]] :: T, DH :: DT] =
      new DocumentedGenericRecord[FieldType[L, Option[H]] :: T, DH :: DT] {
        def record(docs: DH :: DT) =
          (optField(labelHead.value.name, docs.head.map(_.text))(jsonSchemaHead) zip jsonSchemaTail
            .record(docs.tail))
            .xmap[FieldType[L, Option[H]] :: T] {
              case (h, t) => shapelessField[L](h) :: t
            }(ht => (ht.head, ht.tail))
      }

    implicit def consCoproduct[L <: Symbol, H, T <: Coproduct](
        implicit
        labelHead: Witness.Aux[L],
        recordHead: GenericRecord[H],
        taggedTail: GenericTagged[T]
    ): GenericTagged[FieldType[L, H] :+: T] =
      new GenericTagged[FieldType[L, H] :+: T](
        jsonSchema = {
          val taggedHead = recordHead.jsonSchema.tagged(labelHead.value.name)
          taggedHead
            .orElse(taggedTail.jsonSchema)
            .xmap[FieldType[L, H] :+: T] {
              case Left(h)  => Inl(shapelessField[L](h))
              case Right(t) => Inr(t)
            } {
              case Inl(h) => Left(h)
              case Inr(t) => Right(t)
            }
        }
      )

  }

  trait GenericJsonSchemaLowLowPriority { this: GenericJsonSchema.type =>

    @implicitNotFound(
      "Unable to derive an instance of JsonSchema[${A}].\n" +
        "The type ${A} must be a case class. Make sure each field of the case class has an implicit JsonSchema.\n" +
        "You can check that a JsonSchema is available for a type T by compiling the expression " +
        "`implicitly[JsonSchema[T]]`.\n" +
        "If type ${A} is a sealed trait, use `genericTagged` or `genericJsonSchema` instead."
    )
    class GenericRecord[A](val jsonSchema: Record[A])
        extends GenericJsonSchema[A]

    @implicitNotFound(
      "Unable to derive an instance of JsonSchema[${A}].\n" +
        "The type ${A} must be a sealed trait. Make sure it is only extended by case classes, and " +
        "for each case class, make sure each field of the case class has an implicit JsonSchema.\n" +
        "You can check that a JsonSchema is available for a type T by compiling the expression " +
        "`implicitly[JsonSchema[T]]`.\n" +
        "If type ${A} is a case class, use `genericRecord` or `genericJsonSchema` instead."
    )
    class GenericTagged[A](val jsonSchema: Tagged[A])
        extends GenericJsonSchema[A]

    trait DocumentedGenericRecord[A, D <: HList] {
      def record(docs: D): Record[A]
    }

    implicit def recordGeneric[A, R, D <: HList](
        implicit
        gen: LabelledGeneric.Aux[A, R],
        docOpt: GenericDescription[A],
        docAnns: Annotations.Aux[docs, A, D],
        titleOpt: GenericTitle[A],
        record: DocumentedGenericRecord[R, D],
        nameOpt: GenericSchemaName[A]
    ): GenericRecord[A] = {
      val recordA = record.record(docAnns()).xmap[A](gen.from)(gen.to)
      val namedA = nameOpt.value.fold(recordA)(recordA.named(_))
      val docA = docOpt.description.fold(namedA)(namedA.withDescription(_))
      val titleA = titleOpt.title.fold(docA)(docA.withTitle(_))
      new GenericRecord[A](titleA)
    }

    implicit def taggedGeneric[A, R](
        implicit
        gen: LabelledGeneric.Aux[A, R],
        docOpt: GenericDescription[A],
        titleOpt: GenericTitle[A],
        tagged: GenericTagged[R],
        nameOpt: GenericSchemaName[A],
        discriminator: GenericDiscriminatorName[A]
    ): GenericTagged[A] = {
      val taggedA = tagged.jsonSchema.xmap[A](gen.from)(gen.to)
      val namedA = nameOpt.value.fold(taggedA)(taggedA.named(_))
      val discA = namedA.withDiscriminator(discriminator.name)
      val docA = docOpt.description.fold(discA)(discA.withDescription(_))
      val titleA = titleOpt.title.fold(docA)(docA.withTitle(_))
      new GenericTagged[A](titleA)
    }
  }

  /** Internal machinery for deriving the discriminator name of a type */
  trait GenericDiscriminatorNames {

    class GenericDiscriminatorName[A](val name: String)

    object GenericDiscriminatorName
        extends GenericDiscriminatorNameLowPriority {
      implicit def annotated[A](
          implicit ann: Annotation[discriminator, A]
      ): GenericDiscriminatorName[A] =
        new GenericDiscriminatorName(ann().name)
    }

    trait GenericDiscriminatorNameLowPriority {
      implicit def default[A]: GenericDiscriminatorName[A] =
        new GenericDiscriminatorName(defaultDiscriminatorName)
    }

  }

  /** Internal machinery for deriving the schema name of a type */
  trait GenericSchemaNames {

    class GenericSchemaName[A](val value: Option[String])

    object GenericSchemaName extends GenericSchemaNameLowPriority {
      implicit def annotatedName[A](
          implicit ann: Annotation[name, A]
      ): GenericSchemaName[A] =
        new GenericSchemaName(Some(ann().value))

      @nowarn("cat=unused-params")
      implicit def annotatedUnnamed[A](
          implicit ann: Annotation[unnamed, A]
      ): GenericSchemaName[A] =
        new GenericSchemaName(None)
    }

    trait GenericSchemaNameLowPriority {
      implicit def fromClassTag[A](
          implicit ct: ClassTag[A]
      ): GenericSchemaName[A] =
        new GenericSchemaName(Some(classTagToSchemaName(ct)))
    }

  }

  /** Internal machinery for extracting a description of a type */
  trait GenericDescriptions {

    class GenericDescription[A](val description: Option[String])

    object GenericDescription extends GenericDescriptionLowPriority {
      implicit def annotated[A](
          implicit ann: Annotation[docs, A]
      ): GenericDescription[A] =
        new GenericDescription(Some(ann().text))
    }

    trait GenericDescriptionLowPriority {
      implicit def noDescription[A]: GenericDescription[A] =
        new GenericDescription[A](None)
    }
  }

  /** Internal machinery for extracting a title of a type */
  trait GenericTitles {

    class GenericTitle[A](val title: Option[String])

    object GenericTitle extends GenericTitleLowPriority {
      implicit def annotated[A](
          implicit ann: Annotation[title, A]
      ): GenericTitle[A] =
        new GenericTitle(Some(ann().value))
    }

    trait GenericTitleLowPriority {
      implicit def noTitle[A]: GenericTitle[A] =
        new GenericTitle[A](None)
    }
  }

  /**
    * Compute a schema name (used for documentation) based on a `ClassTag`.
    * The provided implementation uses the fully qualified name of the class.
    * This could result in non unique values and mess with documentation.
    *
    * You can override this method to use a custom logic.
    */
  def classTagToSchemaName(ct: ClassTag[_]): String = {
    val jvmName = ct.runtimeClass.getName
    // name fix for case objects
    val name =
      if (jvmName.nonEmpty && jvmName.last == '$') jvmName.init else jvmName
    name.replace('$', '.')
  }

  /** Derives a `JsonSchema[A]` for a type `A`.
    *
    * In a sense, this operation asks shapeless to compute a ''type level'' description
    * of a data type (based on HLists and Coproducts) and turns it into a ''term level''
    * description of the data type (based on the `JsonSchemas` algebra interface)
    *
    * @see [[genericRecord]] for details on how schemas are derived for case classes
    * @see [[genericTagged]] for details on how schemas are derived for sealed traits
    */
  def genericJsonSchema[A](
      implicit genJsonSchema: GenericJsonSchema[A]
  ): JsonSchema[A] =
    genJsonSchema.jsonSchema

  /** Derives a `Record[A]` schema for a case class `A`.
    *
    * The resulting schema:
    *
    *   - describes a JSON object,
    *   - has required properties of the same name and type as each case class field,
    *   - has optional properties of the same name and type as each case class
    *     field of type `Option[X]` for some type `X`,
    *   - includes the description possibly attached to each case class field
    *     via the [[docs @docs]] annotation,
    *   - has a name, computed from a `ClassTag[A]` by the [[classTagToSchemaName]]
    *     operation.
    */
  def genericRecord[A](
      implicit genRecord: GenericJsonSchema.GenericRecord[A]
  ): Record[A] =
    genRecord.jsonSchema

  /** Derives a `Tagged[A]` schema for a sealed trait `A`.
    *
    * The resulting schema:
    *
    *   - is the alternative of the leaf case classes schemas,
    *   - the field used for discriminating the alternatives is defined by the
    *     [[discriminator @discriminator]] annotation, if present on the sealed
    *     trait definition, or by the [[defaultDiscriminatorName]] method otherwise,
    *   - each alternative is discriminated by the name (not qualified) of the
    *     case class.
    */
  def genericTagged[A](
      implicit genTagged: GenericJsonSchema.GenericTagged[A]
  ): Tagged[A] =
    genTagged.jsonSchema

  implicit final class RecordGenericOps[A](record: Record[A]) {
    def as[B](implicit gen: Generic.Aux[A, B]): Record[B] =
      record.xmap(gen.to)(gen.from)
  }

  implicit final class JsonSchemaGenericOps[A](schema: JsonSchema[A]) {
    def as[B](implicit gen: Generic.Aux[A, B]): JsonSchema[B] =
      schema.xmap(gen.to)(gen.from)
  }

  // Summons a `Generic.Aux[T, A]` from a tuple type `T` to an arbitrary
  // type `A` that has the same generic representation as the tuple type `T`
  @nowarn("cat=unused-params")
  implicit def shapelessGenericFromTuple[A, T, L <: HList](
      implicit
      tup: Tupler.Aux[L, T],
      genT: Generic.Aux[T, L],
      genA: Generic.Aux[A, L]
  ): Generic.Aux[T, A] =
    new Generic[T] {
      type Repr = A
      def to(t: T): A = genA.from(genT.to(t))
      def from(a: A): T = genT.from(genA.to(a))
    }

}
