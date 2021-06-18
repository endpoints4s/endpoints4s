package endpoints4s.generic

import endpoints4s.algebra

import scala.compiletime.{ constValue, erasedValue, summonFrom, summonInline }
import scala.deriving.Mirror
import scala.reflect.ClassTag
import scala.util.chaining.given
import shapeless3.deriving.{ Annotation, Annotations }

/** Enriches [[JsonSchemas]] with two kinds of operations:
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
  */
trait JsonSchemas extends algebra.JsonSchemas:

  /** Derives a `JsonSchema[A]` for a type `A`.
    *
    * @see [[genericRecord]] for details on how schemas are derived for case classes
    * @see [[genericTagged]] for details on how schemas are derived for sealed traits
    */
  inline def genericJsonSchema[A](using mirror: Mirror.Of[A]): JsonSchema[A] =
    inline mirror match
      case sum:     Mirror.Sum     => genericTagged(using sum)
      case product: Mirror.Product => genericRecord(using product)

  /** Derives a `Record[A]` schema for a case class `A`.
    *
    * The resulting schema:
    *
    *   - describes a JSON object,
    *   - has required properties of the same name and type as each case class field,
    *   - has optional properties of the same name and type as each case class
    *     field of type `Option[X]` for some type `X`,
    *   - has optional properties of the same name and type as each case class field
    *     with a default value,
    *   - includes the description possibly attached to each case class field
    *     via the [[docs @docs]] annotation,
    *   - has a name, computed from a `ClassTag[A]` by the [[classTagToSchemaName]]
    *     operation, or defined by the [[name @name]], or [[unnamed @unnamed]] annotations.
    */
  inline def genericRecord[A](using productOf: Mirror.ProductOf[A]): Record[A] =
    val docsAnnotations = Annotations[docs, A]
    val defaultValues = Defaults.summonDefaults[A]
    summonRecord[
      productOf.MirroredElemTypes,
      productOf.MirroredElemLabels,
      docsAnnotations.Out,
      defaultValues.DefaultValues
    ](
      docsAnnotations(),
      defaultValues.defaultValues
    ).asInstanceOf[Record[productOf.MirroredElemTypes]] // FIXME Why is this asInstanceOf call necessary?
      .xmap[A](productOf.fromProduct)(a => Tuple.fromProduct(a.asInstanceOf).asInstanceOf)
      .pipe(record => summonName[A].fold(record)(record.named))
      .pipe(record => summonDescription[A].fold(record)(record.withDescription))
      .pipe(record => summonTitle[A].fold(record)(record.withTitle))

  /** Derives a `Tagged[A]` schema for a sealed trait `A`.
    *
    * The resulting schema:
    *
    *   - is the alternative of the leaf case classes schemas,
    *   - the field used for discriminating the alternatives is defined by the
    *     [[discriminator @discriminator]] annotation, if present on the sealed
    *     trait definition, or by the [[defaultDiscriminatorName]] method otherwise,
    *   - each alternative is discriminated by the name (not qualified) of the
    *     case class,
    *   - if the sealed trait is annotated with [[docs @docs]] or [[title @title]],
    *     that information is added to the schema,
    *   - the resulting schema has a named, defined by the annotations [[name @name]]
    *     or [[unnamed @unnamed]], or by calling [[classTagToSchemaName]] with the
    *     trait’s `ClassTag`.
    */
  inline def genericTagged[A](using sumOf: Mirror.SumOf[A]): Tagged[A] =
    summonTagged[sumOf.MirroredElemTypes, sumOf.MirroredElemLabels](0).asInstanceOf[Tagged[sumOf.MirroredElemTypes]] // FIXME Remove asInstanceOf
      .xmap[A](tuple => last(tuple).asInstanceOf[A])(a => asTuple(sumOf.ordinal(a), a).asInstanceOf[sumOf.MirroredElemTypes])
      .pipe(tagged => summonName[A].fold(tagged)(tagged.named))
      .withDiscriminator(summonDiscriminator[A])
      .pipe(tagged => summonDescription[A].fold(tagged)(tagged.withDescription))
      .pipe(tagged => summonTitle[A].fold(tagged)(tagged.withTitle))

  /**
    * Summons an instance of `Record[Types]`
    *
    * @tparam Types          List of record field types (e.g., `(Int, String)`)
    * @tparam Labels         List of record field names’ types (e.g., `"foo".type, "bar".type)`)
    * @tparam Docs           List of the possible `@docs` annotations’ types attached to each of the
    *                        record field (e.g., `(None.type, Some[docs])`)
    * @param  docAnnotations List of the possible `@docs` annotations attached to each of the record
    *                        field (e.g., `(None, Some(docs("Bar field")))`)
    * @tparam DefaultValues  List of the possible default values’ types of the record fields
    *                        (e.g., `(Some[Int], None.type)`)
    * @param  defaultValues  List of the possible default values of the record fields (e.g.,
    *                        `(Some(42), None)`)
    */
  private[generic] inline def summonRecord[
    Types <: Tuple,
    Labels <: Tuple,
    Docs <: Tuple,
    DefaultValues <: Tuple
  ](
    docAnnotations: Docs,
    defaultValues: DefaultValues
  ): Record[Types] =
    inline erasedValue[(Types, Labels)] match
      case (EmptyTuple, EmptyTuple) =>
        emptyTupleRecord.asInstanceOf[Record[Types]]
      case _: (head *: tail, labelHead *: labelsTail) =>
        val (maybeDoc, docsTail) =
          /*inline*/ docAnnotations match
            case docHead *: docsTail =>
              // TODO Remove asInstanceOf, but the compiler crashes if the above line is `case (docHead: Option[docs]) *: docsTail`
              (docHead.asInstanceOf[Option[docs]].map(_.text), docsTail)
        val (maybeDefaultValue, defaultValuesTail) =
          /*inline*/ defaultValues match
            case defaultValueHead *: defaultValuesTail =>
              // TODO Remove asInstanceOf
              (defaultValueHead.asInstanceOf[Option[head]], defaultValuesTail)
        val labelHead = constValue[labelHead].asInstanceOf[String] // TODO Remove asInstanceOf
        val recordHead: Record[head] =
          inline erasedValue[head] match
            // If the field type is an Option, return a schema containing an optional field
            case _: Option[inner] =>
              optField[inner](labelHead, maybeDoc)(using summonInline[JsonSchema[inner]])
                .asInstanceOf[Record[head]] // FIXME Remove asInstanceOf
            case _: head =>
              maybeDefaultValue match
                // If the field is mandatory in the case class but has a default value, return a schema containing an optional field with a default value
                case Some(defaultValue) =>
                  optFieldWithDefault[head](labelHead, defaultValue, maybeDoc)(using summonInline[JsonSchema[head]])
                case None =>
                  field[head](labelHead, maybeDoc)(using summonInline[JsonSchema[head]])
        val recordTail: Record[tail] =
          summonRecord[tail, labelsTail, docsTail.type, defaultValuesTail.type](docsTail, defaultValuesTail)
            .asInstanceOf[Record[tail]] // FIXME Remove asInstanceOf
        recordHead.zip(recordTail)
          .xmap[head *: tail]((h, t) => h *: t) { case h *: t => (h, t) } // TODO Define as a new operation in the algebra
          .asInstanceOf[Record[Types]] // FIXME Remove asIntanceOf

  private val emptyTupleRecord: Record[EmptyTuple] =
    emptyRecord.xmap(_ => EmptyTuple)(_ => ())

  /** Summon the title of a schema for the type `A`. If the type `A`
    * has a [[title @title]] annotation, it is used as a title, otherwise
    * the schema has no title
    */
  private[generic] inline def summonTitle[A]: Option[String] =
    summonFrom {
      case annotation: Annotation[`title`, A] => Some(annotation().value)
      case _                                  => None
    }

  /** Summon the name of a schema for the type `A`. If the type `A`
    * has a [[name @name]] annotation, it is used as a name,
    * if it has a [[unnamed @unnamed]] annotation the schema has no
    * name, otherwise it has the result of calling [[classTagToSchemaName]]
    * with a `ClassTag[A]`.
    */
  private[generic] inline def summonName[A]: Option[String] =
    summonFrom {
      case annotation: Annotation[`name`, A]    => Some(annotation().value)
      case annotation: Annotation[`unnamed`, A] => None
      case classTag: ClassTag[A]                => Some(classTagToSchemaName(classTag))
    }

  /** Summon the description of a schema for the type `A`. If the type `A`
    * has a [[docs @docs]] annotation, it is used as a description,
    * otherwise the schema has no description.
    */
  private[generic] inline def summonDescription[A]: Option[String] =
    summonFrom {
      case annotation: Annotation[`docs`, A] => Some(annotation().text)
      case _                                 => None
    }

  /** Summon the discriminator of a schema for the trait `A`. If the trait `A`
    * has a [[discriminator @discriminator]] annotation, it is used as
    * a discriminator, otherwise we use the [[defaultDiscriminatorName]].
    */
  private[generic] inline def summonDiscriminator[A]: String =
    summonFrom {
      case annotation: Annotation[`discriminator`, A] => annotation().name
      case _                                          => defaultDiscriminatorName
    }

  /** Retrieve the last element of a tuple */
  private def last(tuple: Tuple): Any =
    tuple match
      case h *: EmptyTuple => h
      case () *: t         => last(t)

  /** Build a tuple with `n` elements, the last one containing the value `a` */
  private def asTuple[A](n: Int, a: A): Tuple =
    n match
      case 0 => a *: EmptyTuple
      case _ => () *: asTuple(n - 1, a)

  /** Summon the [[Record]] schema of a concrete type of a sealed trait.
    * If there is a given instance of `GenericJsonSchema.GenericRecord[A]`,
    * it is used as the schema, otherwise we derive the schema.
    */
  private[generic] inline def summonTaggedRecord[A]: Record[A] =
    summonFrom {
      case custom: GenericJsonSchema.GenericRecord[A] => custom.jsonSchema
      case _                                          => genericRecord[A](using summonInline[Mirror.ProductOf[A]])
    }

  /** Summon a [[Tagged]] schema for the alternatives `Types`.
    * @tparam Types  List of record types (e.g. `(Circle, Rectangle)`)
    * @tparam Labels List of record labels’ types (e.g. `("Circle".type, "Rectangle".type)`)
    * @param  i      Index of the head alternative
    *
    * We encode the ordinal `i` of each alternative as a tuple containing `i - 1` `Unit`
    * followed by the actual value.
    */
  private[generic] inline def summonTagged[Types <: Tuple, Labels <: Tuple](i: Int): Tagged[Types] =
    inline erasedValue[(Types, Labels)] match
      // Last alternative
      case _: (head *: EmptyTuple, labelHead *: EmptyTuple) =>
        summonTaggedRecord[head].asInstanceOf[Record[head]] // FIXME Remove asInstanceOf
          .tagged(constValue[labelHead].asInstanceOf[String]) // TODO Remove asInstanceOf
          .xmap[head *: EmptyTuple](h => h *: EmptyTuple) { case h *: EmptyTuple => h }
          .asInstanceOf[Tagged[Types]] // FIXME Remove asInstanceOf
      case _: (head *: tail, labelHead *: labelsTail) =>
        summonTaggedRecord[head].asInstanceOf[Record[head]] // FIXME Remove asInstanceOf
          .tagged(constValue[labelHead].asInstanceOf[String]) // TODO Remove asInstanceOf
          .orElse(summonTagged[tail, labelsTail](i + 1).asInstanceOf[Tagged[tail]]) // FIXME Remove asInstanceOf
          .xmap {
            case Left(h)  => h *: EmptyTuple
            case Right(t) => () *: t
          } {
            case h *: EmptyTuple => Left(h.asInstanceOf[head])
            case () *: t         => Right(t.asInstanceOf[tail])
          }
          .asInstanceOf[Tagged[Types]]

  extension [A <: Product](schema: JsonSchema[A])
    /**
      * Transforms a `JsonSchema[A]` into a `JsonSchema[B]`, if `A` is the “generic”
      * representation of `B`.
      */
    inline def as[B](using mirror: Mirror.ProductOf[B] { type MirroredElemTypes = A }): JsonSchema[B] =
      schema.xmap[B](a => mirror.fromProduct(a))(b => Tuple.fromProduct(b.asInstanceOf[Product]).asInstanceOf[A])

  // Compatibility with Scala 2 API
  object GenericJsonSchema:
    class GenericRecord[A](val jsonSchema: Record[A])
  end GenericJsonSchema

  /** Compute a schema name (used for documentation) based on a `ClassTag`.
    * The provided implementation uses the fully qualified name of the class.
    * This could result in non unique values and mess with documentation.
    *
    * You can override this method to use a custom logic.
    */
  def classTagToSchemaName(ct: ClassTag[?]): String =
    val jvmName = ct.runtimeClass.getName
    // name fix for case objects
    val name =
      if jvmName.nonEmpty && jvmName.last == '$' then jvmName.init
      else jvmName
    name.replace('$', '.')

end JsonSchemas

/**
  * The default values of the primary constructor of the case class `A`.
  *
  * Consider the following case class definition:
  *
  * {{{
  *   case class Foo(x: Int, s: String = "bar")
  * }}}
  *
  * The compiler can summon the following instance of `Defaults[Foo]`:
  *
  * {{{
  *   Defaults[Foo]:
  *     type DefaultValues = (None.type, Some[String])
  *     def defaultValues = (None, Some("bar"))
  * }}}
  *
  */
private trait Defaults[A]:
  /**
    * The (heterogeneous) list of the possible default values of the constructor of `A`.
    * The list has as many elements as the case class fields.
    * Every element type is either `Option[X]`, in case the field (of type `X`) has
    * a default value, or `None.type`, in case the field has no default value.
    */
  type DefaultValues <: Tuple
  /**
    * The (heterogeneous) list of the possible default values of the constructor of `A`.
    */
  def defaultValues: DefaultValues

private object Defaults:

  transparent inline given summonDefaults[A]: Defaults[A] =
    ${ summonDefaultsMacro[A] }

  import scala.quoted.*

  /**
    * @author Nicolas Stucki
    */
  def summonDefaultsMacro[A : Type](using Quotes): Expr[Defaults[A]] =
    import quotes.reflect.*
    val tpe = TypeRepr.of[A]
    tpe.classSymbol match
      case Some(classSymbol) if classSymbol.flags.is(Flags.Case) =>
        val defaultParams =
          for
            method <- classSymbol.companionModule.moduleClass.declaredMethods
            if method.name.startsWith("$lessinit$greater$default$") // find constructor
            DefDef(_, _, _, Some(defaultParam)) = method.tree // assumes single parameter list (i.e. these defs do not have arguments), and -Yretain-trees is enabled
            paramIndex = method.name.split("\\$").last.toInt
          yield
            (paramIndex, defaultParam.asExpr)
        val defaultParamsMap = defaultParams.toMap // index to tree
        val numArgsInConstructor = classSymbol.primaryConstructor.paramSymss.head.size // assumes the constructor has a single parameter list
        val defaults =
          for i <- 1 to numArgsInConstructor yield
            defaultParamsMap.get(i) match
              case Some('{ $default: t }) => '{ Some($default) }
              case _ => '{ None }
        Expr.ofTupleFromSeq(defaults) match
          case '{ type defaultValuesType <: Tuple; $defaultValuesValue : `defaultValuesType` } => // extract most precise type of tuple
            '{
              new Defaults[A]:
                type DefaultValues = defaultValuesType
                val defaultValues: DefaultValues = $defaultValuesValue
            }
      case _ => report.throwError(s"Invalid type ${tpe.show}. Expecting a case class.")
  end summonDefaultsMacro

end Defaults

