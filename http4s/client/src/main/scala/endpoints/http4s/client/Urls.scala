package endpoints.http4s.client

import cats.implicits._
import endpoints.PartialInvariantFunctor
import endpoints.Tupler
import org.http4s.Uri
import endpoints.Validated
import org.http4s.Query
import endpoints.algebra.Documentation
import org.http4s.ParseResult
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

trait Urls extends endpoints.algebra.Urls with StatusCodes {

  type QueryString[A] = A => Query

  trait QueryStringParam[A] {
    def encode(a: A): List[String]
  }

  override def queryStringPartialInvariantFunctor
      : PartialInvariantFunctor[QueryString] =
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

  override def qs[A](name: String, docs: endpoints.algebra.Documentation)(
      implicit value: QueryStringParam[A]
  ): QueryString[A] =
    a => Query.fromPairs(value.encode(a).tupleLeft(name): _*)

  override def optionalQueryStringParam[A](
      implicit param: QueryStringParam[A]
  ): QueryStringParam[Option[A]] = {
    case Some(a) => param.encode(a)
    case _       => List.empty
  }

  override def repeatedQueryStringParam[A, CC[X] <: Iterable[X]](
      implicit
      param: QueryStringParam[A],
      factory: collection.compat.Factory[A, CC[A]]
  ): QueryStringParam[CC[A]] =
    _.iterator.flatMap(param.encode).toList

  override def queryStringParamPartialInvariantFunctor
      : PartialInvariantFunctor[QueryStringParam] =
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
    def encode(a: A): Uri.Path
  }

  override def segmentPartialInvariantFunctor
      : PartialInvariantFunctor[Segment] =
    new PartialInvariantFunctor[Segment] {
      override def xmapPartial[A, B](
          fa: Segment[A],
          f: A => Validated[B],
          g: B => A
      ): Segment[B] =
        b => fa.encode(g(b))
    }

  override def stringSegment: Segment[String] =
    s => URLEncoder.encode(s, UTF_8.name)

  trait Path[A] extends Url[A] {
    final def encodeUrl(value: A): ParseResult[Uri] =
      Uri.fromString(encodePath(value).mkString("/"))
    def encodePath(value: A): List[String]
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
    _ => segment.split("/").toList

  def segment[A](name: String, docs: Documentation)(
      implicit s: Segment[A]
  ): Path[A] = a => s.encode(a) :: Nil

  def remainingSegments(name: String, docs: Documentation): Path[String] =
    _ :: Nil

  def chainPaths[A, B](first: Path[A], second: Path[B])(
      implicit tupler: Tupler[A, B]
  ): Path[tupler.Out] = new Path[tupler.Out] {
    def encodePath(ab: tupler.Out): List[String] = {
      val (a, b) = tupler.unapply(ab)
      first.encodePath(a) ++ second.encodePath(b)
    }
  }

  trait Url[A] {
    def encodeUrl(value: A): ParseResult[Uri]
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

  override def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(
      implicit tupler: Tupler[A, B]
  ): Url[tupler.Out] = { out =>
    val (a, b) = tupler.unapply(out)
    path.encodeUrl(a).map(uri => uri.copy(query = uri.query ++ qs(b).pairs))
  }

}
