package endpoints.ujson

import java.util.UUID

import endpoints.{PartialInvariantFunctor, Tupler, Validated, algebra}
import endpoints.algebra.Encoder

import scala.collection.compat._
import scala.collection.mutable

trait JsonSchemas extends algebra.JsonSchemas with TuplesSchemas {

  trait JsonSchema[A] {
    def codec: Encoder[A, ujson.Value] // Eventually, will be a `Codec[A, Value]`
  }
  trait Record[A] extends JsonSchema[A] {
    def codec: Encoder[A, ujson.Obj] // Result type refined to `Obj`
  }
  case class TagAndObj(discriminatorName: String, tag: String, obj: ujson.Obj)
  class Tagged[A](val tagAndObj: A => TagAndObj) extends Record[A] {
    val codec = value => {
      val TagAndObj(name, tag, json) = tagAndObj(value)
      json(name) = ujson.Str(tag)
      json
    }
  }
  type Enum[A] = JsonSchema[A]

  implicit def jsonSchemaPartialInvFunctor: PartialInvariantFunctor[JsonSchema] =
    new PartialInvariantFunctor[JsonSchema] {
      def xmapPartial[A, B](fa: JsonSchema[A], f: A => Validated[B], g: B => A): JsonSchema[B] =
        new JsonSchema[B] { val codec = b => fa.codec.encode(g(b)) }
    }

  implicit def recordPartialInvFunctor: PartialInvariantFunctor[Record] =
    new PartialInvariantFunctor[Record] {
      def xmapPartial[A, B](fa: Record[A], f: A => Validated[B], g: B => A): Record[B] =
        new Record[B] { val codec = b => fa.codec.encode(g(b)) }
    }

  implicit def taggedPartialInvFunctor: PartialInvariantFunctor[Tagged] =
    new PartialInvariantFunctor[Tagged] {
      def xmapPartial[A, B](fa: Tagged[A], f: A => Validated[B], g: B => A): Tagged[B] =
        new Tagged(fa.tagAndObj compose g)
    }

  def enumeration[A](values: Seq[A])(f: A => String)(implicit tpe: JsonSchema[String]): Enum[A] =
    new JsonSchema[A] {
      val codec = value => tpe.codec.encode(f(value))
    }

  def namedRecord[A](schema: Record[A], name: String): Record[A] = schema

  def namedTagged[A](schema: Tagged[A], name: String): Tagged[A] = schema

  def namedEnum[A](schema: Enum[A], name: String): Enum[A] = schema

  def lazyRecord[A](schema: => Record[A], name: String): JsonSchema[A] = new JsonSchema[A] {
    val codec = value => schema.codec.encode(value)
  }

  def lazyTagged[A](schema: => Tagged[A], name: String): JsonSchema[A] = new JsonSchema[A] {
    val codec = value => schema.codec.encode(value)
  }

  lazy val emptyRecord: Record[Unit] = new Record[Unit] {
    val codec = _ => ujson.Obj()
  }

  def field[A](name: String, documentation: Option[String] = None)(implicit tpe: JsonSchema[A]): Record[A] =
    new Record[A] {
      val codec = value => ujson.Obj(name -> tpe.codec.encode(value))
    }


  def optField[A](name: String, documentation: Option[String] = None)(implicit tpe: JsonSchema[A]): Record[Option[A]] =
    new Record[Option[A]] {
      val codec = new Encoder[Option[A], ujson.Obj] {
        def encode(maybeValue: Option[A]) = maybeValue match {
          case None => ujson.Obj()
          case Some(value) => ujson.Obj(name -> tpe.codec.encode(value))
        }
      }
    }

  def taggedRecord[A](recordA: Record[A], tag: String): Tagged[A] =
    new Tagged(value => TagAndObj(defaultDiscriminatorName, tag, recordA.codec.encode(value)))

  def withDiscriminatorTagged[A](tagged: Tagged[A], discriminatorName: String): Tagged[A] =
    new Tagged(value => {
      val TagAndObj(_, tag, obj) = tagged.tagAndObj(value)
      TagAndObj(discriminatorName, tag, obj)
    })

  def choiceTagged[A, B](taggedA: Tagged[A], taggedB: Tagged[B]): Tagged[Either[A, B]] =
    new Tagged({
      case Left(value)  => taggedA.tagAndObj(value)
      case Right(value) => taggedB.tagAndObj(value)
    })

  def zipRecords[A, B](recordA: Record[A], recordB: Record[B])(implicit t: Tupler[A, B]): Record[t.Out] =
    new Record[t.Out] {
      val codec = new Encoder[t.Out, ujson.Obj] {
        def encode(from: t.Out): ujson.Obj = {
          val (a, b) = t.unapply(from)
          new ujson.Obj(recordA.codec.encode(a).value ++ recordB.codec.encode(b).value)
        }
      }
    }

  implicit def uuidJsonSchema: JsonSchema[UUID] = new JsonSchema[UUID] {
    val codec = uuid => ujson.Str(uuid.toString)
  }

  implicit def stringJsonSchema: JsonSchema[String] = new JsonSchema[String] {
    val codec = ujson.Str(_)
  }

  implicit def intJsonSchema: JsonSchema[Int] = new JsonSchema[Int] {
    val codec = n => ujson.Num(n.toDouble)
  }

  implicit def longJsonSchema: JsonSchema[Long] = new JsonSchema[Long] {
    val codec = n => ujson.Num(n.toDouble)
  }

  implicit def bigdecimalJsonSchema: JsonSchema[BigDecimal] = new JsonSchema[BigDecimal] {
    val codec = x => ujson.Num(x.doubleValue)
  }

  implicit def floatJsonSchema: JsonSchema[Float] = new JsonSchema[Float] {
    val codec = x => ujson.Num(x.toDouble)
  }

  implicit def doubleJsonSchema: JsonSchema[Double] = new JsonSchema[Double] {
    val codec = ujson.Num(_)
  }

  implicit def booleanJsonSchema: JsonSchema[Boolean] = new JsonSchema[Boolean] {
    val codec = ujson.Bool(_)
  }

  implicit def byteJsonSchema: JsonSchema[Byte] = new JsonSchema[Byte] {
    val codec = b => ujson.Num(b.toDouble)
  }

  implicit def arrayJsonSchema[C[X] <: Seq[X], A](implicit
    jsonSchema: JsonSchema[A],
    factory: Factory[A, C[A]]
  ): JsonSchema[C[A]] = new JsonSchema[C[A]] {
    val codec = as => ujson.Arr(as.map(jsonSchema.codec.encode): _*)
  }

  implicit def mapJsonSchema[A](implicit jsonSchema: JsonSchema[A]): JsonSchema[Map[String, A]] =
    new JsonSchema[Map[String, A]] {
      val codec = map => {
        new ujson.Obj(mutable.LinkedHashMap(map.map { case (k, v) => (k, jsonSchema.codec.encode(v)) }.toSeq: _*))
      }
    }
}

object JsonSchemas extends JsonSchemas
