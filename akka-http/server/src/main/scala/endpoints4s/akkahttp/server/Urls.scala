package endpoints4s.akkahttp.server

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

import scala.collection.compat._
import akka.http.scaladsl.server._
import endpoints4s.algebra.Documentation
import endpoints4s.{Invalid, PartialInvariantFunctor, Tupler, Valid, Validated, algebra}

import scala.collection.mutable

/** [[algebra.Urls]] interpreter that decodes and encodes URLs.
  *
  * @group interpreters
  */
trait Urls extends algebra.Urls with StatusCodes {
  this: EndpointsWithCustomErrors =>

  trait Path[A] extends Url[A] {
    def validate(segments: List[String]): Option[(Validated[A], List[String])]
    final def validateUrl(
        path: List[String],
        query: Map[String, List[String]]
    ): Option[Validated[A]] =
      validate(path).flatMap {
        case (validA, Nil) => Some(validA)
        case (_, _)        => None
      }
  }

  implicit lazy val pathPartialInvariantFunctor: PartialInvariantFunctor[Path] =
    new PartialInvariantFunctor[Path] {
      def xmapPartial[A, B](
          fa: Path[A],
          f: A => Validated[B],
          g: B => A
      ): Path[B] =
        segments =>
          fa.validate(segments).map { case (validA, ss) =>
            (validA.flatMap(f), ss)
          }
      override def xmap[A, B](fa: Path[A], f: A => B, g: B => A): Path[B] =
        segments => fa.validate(segments).map { case (validA, ss) => (validA.map(f), ss) }
    }

  trait Url[A] {
    def validateUrl(
        segments: List[String],
        query: Map[String, List[String]]
    ): Option[Validated[A]]

    final def directive: Directive1[A] = {
      (Directives.path(Directives.Segments) & Directives.parameterMultiMap)
        .tflatMap { case (segments, query) =>
          validateUrl(segments, query) match {
            case None               => Directives.reject
            case Some(inv: Invalid) => handleClientErrors(inv)
            case Some(Valid(a))     => Directives.provide(a)
          }
        }
    }
  }

  trait QueryString[T] {
    def validate(params: Map[String, List[String]]): Validated[T]
  }

  implicit lazy val queryStringPartialInvariantFunctor: PartialInvariantFunctor[QueryString] =
    new PartialInvariantFunctor[QueryString] {
      def xmapPartial[A, B](
          fa: QueryString[A],
          f: A => Validated[B],
          g: B => A
      ): QueryString[B] =
        params => fa.validate(params).flatMap(f)
      override def xmap[A, B](
          fa: QueryString[A],
          f: A => B,
          g: B => A
      ): QueryString[B] =
        params => fa.validate(params).map(f)
    }

  /** Given a parameter name and a query string content, returns a decoded parameter
    * value of type `T`, or `Invalid` if decoding failed
    */
  type QueryStringParam[T] = (String, Map[String, Seq[String]]) => Validated[T]

  implicit lazy val queryStringParamPartialInvariantFunctor
      : PartialInvariantFunctor[QueryStringParam] =
    new PartialInvariantFunctor[QueryStringParam] {
      def xmapPartial[A, B](
          fa: QueryStringParam[A],
          f: A => Validated[B],
          g: B => A
      ): QueryStringParam[B] =
        (name, qs) => fa(name, qs).flatMap(f)
      override def xmap[A, B](
          fa: QueryStringParam[A],
          f: A => B,
          g: B => A
      ): QueryStringParam[B] =
        (name, qs) => fa(name, qs).map(f)
    }

  trait Segment[A] {
    def validate(s: String): Validated[A]
  }

  implicit lazy val segmentPartialInvariantFunctor: PartialInvariantFunctor[Segment] =
    new PartialInvariantFunctor[Segment] {
      def xmapPartial[A, B](
          fa: Segment[A],
          f: A => Validated[B],
          g: B => A
      ): Segment[B] =
        s => fa.validate(s).flatMap(f)
      override def xmap[A, B](
          fa: Segment[A],
          f: A => B,
          g: B => A
      ): Segment[B] =
        s => fa.validate(s).map(f)
    }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit
      tupler: Tupler[A, B]
  ): Url[tupler.Out] = { (segments: List[String], query: Map[String, List[String]]) =>
    path.validate(segments).flatMap {
      case (validA, Nil) => Some(validA.zip(qs.validate(query))(tupler))
      case (_, _)        => None
    }
  }

  //***************
  // Query strings
  //***************

  implicit lazy val stringQueryString: QueryStringParam[String] =
    (name, map) => {
      val maybeValue = map.get(name).flatMap(_.headOption)
      Validated.fromOption(maybeValue)("Missing value")
    }

  def qs[A](name: String, docs: Documentation)(implicit
      param: QueryStringParam[A]
  ): QueryString[A] = { kvs =>
    param(name, kvs).mapErrors(
      _.map(error => s"$error for query parameter '$name'")
    )
  }

  implicit def optionalQueryStringParam[A](implicit
      param: QueryStringParam[A]
  ): QueryStringParam[Option[A]] =
    (name, qs) =>
      qs.get(name) match {
        case None    => Valid(None)
        case Some(_) => param(name, qs).map(Some(_))
      }

  implicit def repeatedQueryStringParam[A, CC[X] <: Iterable[X]](implicit
      param: QueryStringParam[A],
      factory: Factory[A, CC[A]]
  ): QueryStringParam[CC[A]] =
    (name, qs) =>
      qs.get(name) match {
        case None => Valid(factory.newBuilder.result())
        case Some(vs) =>
          vs.foldLeft[Validated[mutable.Builder[A, CC[A]]]](
            Valid(factory.newBuilder)
          ) {
            case (inv: Invalid, v) =>
              // Pretend that this was the query string and delegate to the `A` query string param
              param(name, Map(name -> (v :: Nil)))
                .fold(_ => inv, errors => Invalid(inv.errors ++ errors))
            case (Valid(b), v) =>
              // Pretend that this was the query string and delegate to the `A` query string param
              param(name, Map(name -> (v :: Nil))).map(b += _)
          }.map(_.result())
      }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit
      tupler: Tupler[A, B]
  ): QueryString[tupler.Out] = { (params: Map[String, List[String]]) =>
    first.validate(params).zip(second.validate(params))
  }

  implicit lazy val urlPartialInvariantFunctor: PartialInvariantFunctor[Url] =
    new PartialInvariantFunctor[Url] {
      def xmapPartial[A, B](
          fa: Url[A],
          f: A => Validated[B],
          g: B => A
      ): Url[B] = { (segments: List[String], query: Map[String, List[String]]) =>
        fa.validateUrl(segments, query).map(_.flatMap(f))
      }
      override def xmap[A, B](fa: Url[A], f: A => B, g: B => A): Url[B] = {
        (segments: List[String], query: Map[String, List[String]]) =>
          fa.validateUrl(segments, query).map(_.map(f))
      }
    }

  // ********
  // Paths
  // ********

  implicit def stringSegment: Segment[String] = Valid(_)

  def segment[A](name: String, docs: Documentation)(implicit
      s: Segment[A]
  ): Path[A] = {
    case head :: tail =>
      val validatedA =
        s.validate(head)
          .mapErrors(
            _.map(error => s"$error for segment${if (name.isEmpty) "" else s" '$name'"}")
          )
      Some((validatedA, tail))
    case Nil => None
  }

  def remainingSegments(name: String, docs: Documentation): Path[String] = { segments =>
    if (segments.isEmpty) None
    else
      Some(
        (
          Valid(
            segments.map(URLEncoder.encode(_, UTF_8.name())).mkString("/")
          ),
          Nil
        )
      )
  }

  def staticPathSegment(segment: String): Path[Unit] = { segments =>
    if (segment.isEmpty) Some((Valid(()), segments))
    else {
      segments match {
        case `segment` :: tail => Some((Valid(()), tail))
        case _                 => None
      }
    }
  }

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit
      tupler: Tupler[A, B]
  ): Path[tupler.Out] =
    (p1: List[String]) => {
      first.validate(p1).flatMap { case (validA, p2) =>
        second.validate(p2).map { case (validB, p3) =>
          (validA.zip(validB)(tupler), p3)
        }
      }
    }

  /** Simpler alternative to `Directive.&()` method
    */
  protected def joinDirectives[T1, T2](
      dir1: Directive1[T1],
      dir2: Directive1[T2]
  )(implicit tupler: Tupler[T1, T2]): Directive1[tupler.Out] = {
    Directive[Tuple1[tupler.Out]] { inner =>
      dir1.tapply { case Tuple1(prefix) =>
        dir2.tapply { case Tuple1(suffix) =>
          inner(Tuple1(tupler(prefix, suffix)))
        }
      }
    }
  }

  protected def convToDirective1(directive: Directive0): Directive1[Unit] = {
    directive.tmap(_ => Tuple1(()))
  }

  /** This method is called by endpoints4s when decoding a request failed.
    *
    * The provided implementation calls `clientErrorsResponse` to complete
    * with a response containing the errors.
    *
    * This method can be overridden to customize the error reporting logic.
    */
  def handleClientErrors(invalid: Invalid): StandardRoute =
    StandardRoute(clientErrorsResponse(invalidToClientErrors(invalid)))

}
