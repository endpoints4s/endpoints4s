package endpoints4s.http4s.client

import cats.implicits._
import endpoints4s.PartialInvariantFunctor
import endpoints4s.Tupler
import org.http4s.Uri
import endpoints4s.Validated
import org.http4s.Query
import endpoints4s.algebra.Documentation

trait Urls extends endpoints4s.algebra.Urls with StatusCodes {

  type QueryString[A] = A => Query

  trait QueryStringParam[A] {
    def encode(a: A): List[String]
  }

  def oneOfQueryStringParam[A,B](qspa: QueryStringParam[A], qspb: QueryStringParam[B]): QueryStringParam[Either[A,B]] = new QueryStringParam[Either[A,B]] {
    def encode(either: Either[A,B]):List[String] = either.fold(qspa.encode, qspb.encode)
  }

  override def queryStringPartialInvariantFunctor: PartialInvariantFunctor[QueryString] =
    new PartialInvariantFunctor[QueryString] {
      override def xmapPartial[A, B](
          fa: A => Query,
          f: A => Validated[B],
          g: B => A
      ): B => Query =
        b => fa(g(b))
    }

  override def combineQueryStrings[A, B](
      first: QueryString[A],
      second: QueryString[B]
  )(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] = { out =>
    val (a, b) = tupler.unapply(out)

    first(a) ++ second(b).pairs
  }

  override def qs[A](name: String, docs: endpoints4s.algebra.Documentation)(implicit
      value: QueryStringParam[A]
  ): QueryString[A] =
    a => Query.fromPairs(value.encode(a).tupleLeft(name): _*)

  type WithDefault[A] = Option[A]

  override def optQsWithDefault[A](name: String, default: A, docs: Documentation = None)(implicit
      value: QueryStringParam[A]
  ): QueryString[WithDefault[A]] =
    qs(name, docs)(optionalQueryStringParam(value))

  override def optionalQueryStringParam[A](implicit
      param: QueryStringParam[A]
  ): QueryStringParam[Option[A]] = {
    case Some(a) => param.encode(a)
    case _       => List.empty
  }

  override def repeatedQueryStringParam[A, CC[X] <: Iterable[X]](implicit
      param: QueryStringParam[A],
      factory: collection.compat.Factory[A, CC[A]]
  ): QueryStringParam[CC[A]] =
    _.iterator.flatMap(param.encode).toList

  override def queryStringParamPartialInvariantFunctor: PartialInvariantFunctor[QueryStringParam] =
    new PartialInvariantFunctor[QueryStringParam] {
      override def xmapPartial[A, B](
          fa: QueryStringParam[A],
          f: A => Validated[B],
          g: B => A
      ): QueryStringParam[B] =
        b => fa.encode(g(b))
    }

  override def stringQueryString: QueryStringParam[String] =
    s => s :: Nil

  trait Segment[A] {
    def encode(a: A): Uri.Path.Segment
  }

  override def segmentPartialInvariantFunctor: PartialInvariantFunctor[Segment] =
    new PartialInvariantFunctor[Segment] {
      override def xmapPartial[A, B](
          fa: Segment[A],
          f: A => Validated[B],
          g: B => A
      ): Segment[B] =
        b => fa.encode(g(b))
    }

  override def stringSegment: Segment[String] =
    s => Uri.Path.Segment(s)

  trait Path[A] extends Url[A] {
    final def encodeUrl(value: A): (Uri.Path, Query) =
      (Uri.Path(encodePath(value).dropWhile(_ == Uri.Path.Segment(""))), Query.empty)

    def encodePath(value: A): Vector[Uri.Path.Segment]
  }

  implicit lazy val pathPartialInvariantFunctor: PartialInvariantFunctor[Path] =
    new PartialInvariantFunctor[Path] {
      def xmapPartial[A, B](
          fa: Path[A],
          f: A => Validated[B],
          g: B => A
      ): Path[B] = (b: B) => fa.encodePath(g(b))
    }

  def staticPathSegment(segment: String): Path[Unit] =
    _ => segment.split("/").map(Uri.Path.Segment).toVector

  def segment[A](name: String, docs: Documentation)(implicit
      s: Segment[A]
  ): Path[A] = a => Vector(s.encode(a))

  def remainingSegments(name: String, docs: Documentation): Path[String] =
    segments => Vector(Uri.Path.Segment.encoded(segments))

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit
      tupler: Tupler[A, B]
  ): Path[tupler.Out] =
    new Path[tupler.Out] {
      def encodePath(ab: tupler.Out): Vector[Uri.Path.Segment] = {
        val (a, b) = tupler.unapply(ab)
        first.encodePath(a) ++ second.encodePath(b)
      }
    }

  trait Url[A] {
    def encodeUrl(value: A): (Uri.Path, Query)
  }

  override def urlPartialInvariantFunctor: PartialInvariantFunctor[Url] =
    new PartialInvariantFunctor[Url] {
      override def xmapPartial[A, B](
          fa: Url[A],
          f: A => Validated[B],
          g: B => A
      ): Url[B] =
        b => fa.encodeUrl(g(b))
    }

  override def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit
      tupler: Tupler[A, B]
  ): Url[tupler.Out] = { out =>
    val (a, b) = tupler.unapply(out)
    val (p, _) = path.encodeUrl(a)
    (Uri.Path(p.segments.dropWhile(_ == Uri.Path.Segment(""))), qs(b))
  }

}
