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

  inline def genericJsonSchema[A](using mirror: Mirror.Of[A]): JsonSchema[A] =
    inline mirror match
      case sum:     Mirror.Sum     => genericTagged(using sum)
      case product: Mirror.Product => genericRecord(using product)

  // Type-level String extractor
  private type Str[A <: String]

  private val emptyTupleRecord: Record[EmptyTuple] =
    emptyRecord.xmap(_ => EmptyTuple)(_ => ())

  private[generic] inline def summonTitle[A]: Option[String] =
    summonFrom {
      case annotation: Annotation[`title`, A] => Some(annotation().value)
      case _                                  => None
    }

  private[generic] inline def summonName[A]: Option[String] =
    summonFrom {
      case annotation: Annotation[`name`, A]    => Some(annotation().value)
      case annotation: Annotation[`unnamed`, A] => None
      case classTag: ClassTag[A]                => Some(classTagToSchemaName(classTag))
    }

  private[generic] inline def summonDescription[A]: Option[String] =
    summonFrom {
      case annotation: Annotation[`docs`, A] => Some(annotation().text)
      case _                                 => None
    }

  private[generic] inline def summonDiscriminator[A]: String =
    summonFrom {
      case annotation: Annotation[`discriminator`, A] => annotation().name
      case _                                          => defaultDiscriminatorName
    }

  inline def genericRecord[A](using productOf: Mirror.ProductOf[A]): Record[A] =
    val docsAnnotations = Annotations[docs, A]
    val defaultValues = Defaults.summonDefaults[A]
    summonRecord[productOf.MirroredElemTypes, productOf.MirroredElemLabels, docsAnnotations.Out, defaultValues.DefaultValues](docsAnnotations(), defaultValues.defaultValues).asInstanceOf[Record[productOf.MirroredElemTypes]]
      .xmap[A](productOf.fromProduct)(a => Tuple.fromProduct(a.asInstanceOf).asInstanceOf)
      .pipe(record => summonName[A].fold(record)(record.named))
      .pipe(record => summonDescription[A].fold(record)(record.withDescription))
      .pipe(record => summonTitle[A].fold(record)(record.withTitle))

  private[generic] inline def summonRecord[Types <: Tuple, Labels <: Tuple, Docs <: Tuple, DefaultValues <: Tuple](docAnnotations: Docs, defaultValues: DefaultValues): Record[Types] =
    inline erasedValue[(Types, Labels)] match
      case (EmptyTuple, EmptyTuple) =>
        emptyTupleRecord.asInstanceOf[Record[Types]]
      case _: (head *: tail, /*Str[*/labelHead/*]*/ *: labelsTail) =>
        val (maybeDoc, docsTail) =
          /*inline*/ docAnnotations match
            case docHead *: docsTail =>
              (docHead.asInstanceOf[Option[docs]].map(_.text), docsTail)
        val (maybeDefaultValue, defaultValuesTail) =
          /*inline*/ defaultValues match
            case defaultValueHead *: defaultValuesTail =>
              (defaultValueHead.asInstanceOf[Option[head]], defaultValuesTail)
        val recordHead: Record[head] =
          inline erasedValue[head] match
            case _: Option[inner] =>
              optField[inner](constValue[labelHead].asInstanceOf[String], maybeDoc)(using summonInline[JsonSchema[inner]]).asInstanceOf[Record[head]] // TODO Remove asInstanceOf
            case _: head =>
              maybeDefaultValue match
                case Some(defaultValue) =>
                  optFieldWithDefault[head](constValue[labelHead].asInstanceOf[String], defaultValue, maybeDoc)(using summonInline[JsonSchema[head]])
                case None =>
                  field[head](constValue[labelHead].asInstanceOf[String], maybeDoc)(using summonInline[JsonSchema[head]])
        val recordTail: Record[tail] = summonRecord[tail, labelsTail, docsTail.type, defaultValuesTail.type](docsTail, defaultValuesTail).asInstanceOf[Record[tail]]
        recordHead.cons(recordTail).asInstanceOf[Record[Types]]

  inline def genericTagged[A](using sumOf: Mirror.SumOf[A]): Tagged[A] =
    summonTagged[sumOf.MirroredElemTypes, sumOf.MirroredElemLabels](0).asInstanceOf[Tagged[sumOf.MirroredElemTypes]]
      .xmap[A](tuple => last(tuple).asInstanceOf[A])(a => asTuple(sumOf.ordinal(a), a).asInstanceOf[sumOf.MirroredElemTypes])
      .pipe(tagged => summonName[A].fold(tagged)(tagged.named))
      .withDiscriminator(summonDiscriminator[A])
      .pipe(tagged => summonDescription[A].fold(tagged)(tagged.withDescription))
      .pipe(tagged => summonTitle[A].fold(tagged)(tagged.withTitle))

  private def last(tuple: Tuple): Any =
    tuple match
      case h *: EmptyTuple => h
      case () *: t         => last(t)

  private def asTuple[A](n: Int, a: A): Tuple =
    n match
      case 0 => a *: EmptyTuple
      case _ => () *: asTuple(n - 1, a)

  private[generic] inline def summonTaggedRecord[A]: Record[A] =
    summonFrom {
      case custom: GenericJsonSchema.GenericRecord[A] => custom.jsonSchema
      case _                                          => genericRecord[A](using summonInline)
    }

  private[generic] inline def summonTagged[Types <: Tuple, Labels <: Tuple](i: Int): Tagged[Types] =
    inline erasedValue[(Types, Labels)] match
      case _: (head *: EmptyTuple, labelHead *: EmptyTuple) =>
        summonTaggedRecord[head].asInstanceOf[Record[head]].tagged(constValue[labelHead].asInstanceOf[String])
          .xmap[head *: EmptyTuple](h => h *: EmptyTuple) { case h *: EmptyTuple => h }
          .asInstanceOf[Tagged[Types]]
      case _: (head *: tail, labelHead *: labelsTail) =>
        summonTaggedRecord[head].asInstanceOf[Record[head]].tagged(constValue[labelHead].asInstanceOf[String])
          .orElse(summonTagged[tail, labelsTail](i + 1).asInstanceOf[Tagged[tail]])
          .xmap {
            case Left(h)  => h *: EmptyTuple
            case Right(t) => () *: t
          } {
            case h *: EmptyTuple => Left(h.asInstanceOf[head])
            case () *: t         => Right(t.asInstanceOf[tail])
          }
          .asInstanceOf[Tagged[Types]]

  extension [A <: Product](schema: JsonSchema[A])
    inline def as[B](using mirror: Mirror.ProductOf[B] { type MirroredElemTypes = A }): JsonSchema[B] =
      schema.xmap[B](a => mirror.fromProduct(a))(b => Tuple.fromProduct(b.asInstanceOf[Product]).asInstanceOf[A])

  // Eventually, move this method to the main algebra
  extension [A](recordA: Record[A])
    inline def cons[B <: Tuple](recordB: Record[B]): Record[A *: B] =
      recordA.zip(recordB)
        .xmap[A *: B]((a, b) => a *: b) { case a *: b => (a, b) }

  // Compatibility with Scala 2 API (Needed for tests only?)
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

trait SumElem[F[X]]:
  type Elem
  def tag: String
  def instance: F[Elem]

object SumElem:
  def apply[A, F[X]](fa: F[A], tagValue: String): SumElem[F] =
    new SumElem[F]:
      type Elem = A
      def instance: F[A] = fa
      def tag = tagValue

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
trait Defaults[A]:
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

object Defaults:

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
            meth <- classSymbol.companionModule.moduleClass.declaredMethods
            if meth.name.startsWith("$lessinit$greater$default$")
            DefDef(_, _, _, rhs) = meth.tree // assumes single parameter list (i.e. these defs do not have arguments)
            Some(defaultParam) = rhs // assumes -Yretain-trees is enabled
            paramIndex = meth.name.split("\\$").last.toInt
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
          case '{ type dv <: Tuple; $defaultsTuple : `dv` } => // extract most precise type of tuple
            '{
              new Defaults[A]:
                type DefaultValues = dv
                val defaultValues: DefaultValues = $defaultsTuple
            }
      case _ => report.throwError(s"Invalid type ${tpe.show}. Expecting a case class.")
  end summonDefaultsMacro

end Defaults

