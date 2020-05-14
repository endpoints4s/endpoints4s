package endpoints.scalaj.client

import java.net.URLEncoder

import scala.collection.compat.Factory
import endpoints.{PartialInvariantFunctor, Tupler, Validated, algebra}
import endpoints.algebra.Documentation
import scalaj.http.{Http, HttpRequest}

/**
  * @group interpreters
  */
trait Urls extends algebra.Urls {

  def protocol: String = "http://"

  def address: String

  type QueryString[A] = A => Seq[(String, String)]
  type QueryStringParam[A] = A => List[String]
  type Segment[A] = A => String

  case class Path[A](toStr: A => String)
      extends Url(toStr.andThen(url => Http(protocol + address + "/" + url)))

  implicit lazy val pathPartialInvariantFunctor: PartialInvariantFunctor[Path] =
    new PartialInvariantFunctor[Path] {
      def xmapPartial[A, B](
          fa: Path[A],
          f: A => Validated[B],
          g: B => A
      ): Path[B] = Path(fa.toStr compose g)
    }

  class Url[A](val toReq: A => HttpRequest)

  implicit lazy val queryStringPartialInvariantFunctor
      : PartialInvariantFunctor[QueryString] =
    new PartialInvariantFunctor[QueryString] {
      def xmapPartial[A, B](
          fa: QueryString[A],
          f: A => Validated[B],
          g: B => A
      ): QueryString[B] = fa compose g
    }

  implicit lazy val queryStringParamPartialInvariantFunctor
      : PartialInvariantFunctor[QueryStringParam] =
    new PartialInvariantFunctor[QueryStringParam] {
      def xmapPartial[A, B](
          fa: QueryStringParam[A],
          f: A => Validated[B],
          g: B => A
      ): QueryStringParam[B] = fa compose g
    }

  implicit def stringQueryString: QueryStringParam[String] = _ :: Nil

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(
      implicit tupler: Tupler[A, B]
  ): QueryString[tupler.Out] = { ab =>
    {
      val (a, b) = tupler.unapply(ab)
      first(a) ++ second(b)
    }
  }

  implicit lazy val segmentPartialInvariantFunctor
      : PartialInvariantFunctor[Segment] =
    new PartialInvariantFunctor[Segment] {
      def xmapPartial[A, B](
          fa: Segment[A],
          f: A => Validated[B],
          g: B => A
      ): Segment[B] = fa compose g
    }

  implicit def stringSegment: Segment[String] =
    s => URLEncoder.encode(s, "utf8")

  def qs[A](name: String, docs: Documentation)(
      implicit value: QueryStringParam[A]
  ): QueryString[A] =
    a => value(a).map(name -> _)

  implicit def optionalQueryStringParam[A](
      implicit param: QueryStringParam[A]
  ): QueryStringParam[Option[A]] = {
    case Some(a) => param(a)
    case None    => Nil
  }

  implicit def repeatedQueryStringParam[A, CC[X] <: Iterable[X]](
      implicit param: QueryStringParam[A],
      factory: Factory[A, CC[A]]
  ): QueryStringParam[CC[A]] =
    as => as.iterator.flatMap(param).toList

  def staticPathSegment(segment: String): Path[Unit] = Path(_ => segment)

  def segment[A](name: String, docs: Documentation)(
      implicit s: Segment[A]
  ): Path[A] = Path(s)

  def remainingSegments(name: String, docs: Documentation): Path[String] =
    Path(s => s)

  def chainPaths[A, B](first: Path[A], second: Path[B])(
      implicit tupler: Tupler[A, B]
  ): Path[tupler.Out] = {
    Path(ab => {
      val (a, b) = tupler.unapply(ab)
      val firstStr = first.toStr(a)
      val secondStr = second.toStr(b)
      val separator = if (firstStr.isEmpty || secondStr.isEmpty) "" else "/"
      firstStr + separator + secondStr
    })
  }

  /** Builds an URL from the given path and query string */
  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(
      implicit tupler: Tupler[A, B]
  ): Url[tupler.Out] = {
    new Url(ab => {
      val (a, b) = tupler.unapply(ab)
      path
        .toReq(a)
        .params(qs(b))
    })
  }

  implicit lazy val urlPartialInvariantFunctor: PartialInvariantFunctor[Url] =
    new PartialInvariantFunctor[Url] {
      def xmapPartial[A, B](
          fa: Url[A],
          f: A => Validated[B],
          g: B => A
      ): Url[B] = new Url((b: B) => fa.toReq(g(b)))
    }

}
