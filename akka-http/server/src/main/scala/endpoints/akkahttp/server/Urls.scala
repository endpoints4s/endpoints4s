package endpoints.akkahttp.server

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

import scala.collection.compat._
import scala.language.higherKinds
import akka.http.scaladsl.server._
import endpoints.algebra.Documentation
import endpoints.{PartialInvariantFunctor, Tupler, algebra}

import scala.collection.mutable

/**
  * [[algebra.Urls]] interpreter that decodes and encodes URLs.
  *
  * @group interpreters
  */
trait Urls extends algebra.Urls {

  import akka.http.scaladsl.server.Directives._

  class Path[T](val pathPrefix: Directive1[T]) extends Url[T](
    // Make sure that `this` path is an URL that has no remaining path segments
    joinDirectives(pathPrefix, Directives.pathEndOrSingleSlash.tmap(Tuple1(_)))
  )

  implicit lazy val pathPartialInvariantFunctor: PartialInvariantFunctor[Path] = new PartialInvariantFunctor[Path] {
    def xmapPartial[A, B](fa: Path[A], f: A => Option[B], g: B => A): Path[B] =
      new Path(fa.pathPrefix.flatMap { a =>
        f(a) match {
          case Some(b) => Directives.provide(b)
          case None    => malformedRequest
        }
      })
    override def xmap[A, B](fa: Path[A], f: A => B, g: B => A): Path[B] =
      new Path(fa.directive.map(f))
  }

  class Url[T](val directive: Directive1[T])

  class QueryString[T](val directive: Directive1[T])

  /**
    * @inheritdoc
    *
    * Given a parameter name and a query string content, returns a decoded parameter
    * value of type `T`, or `None` if decoding failed
    */
  type QueryStringParam[T] = (String, Map[String, Seq[String]]) => Option[T]

  implicit lazy val queryStringParamPartialInvFunctor: PartialInvariantFunctor[QueryStringParam] = new PartialInvariantFunctor[QueryStringParam] {
    def xmapPartial[A, B](fa: QueryStringParam[A], f: A => Option[B], g: B => A): QueryStringParam[B] =
      (name, qs) => fa(name, qs).flatMap(f)
    override def xmap[A, B](fa: QueryStringParam[A], f: A => B, g: B => A): QueryStringParam[B] =
      (name, qs) => fa(name, qs).map(f)
  }
  def refineQueryStringParam[A, B](pa: QueryStringParam[A])(f: A => Option[B])(g: B => A): QueryStringParam[B] =
    (name, map) => pa(name, map).flatMap(f)

  type Segment[T] = PathMatcher1[T]

  implicit lazy val segmentPartialInvFunctor: PartialInvariantFunctor[Segment] = new PartialInvariantFunctor[Segment] {
    def xmapPartial[A, B](fa: Segment[A], f: A => Option[B], g: B => A): Segment[B] =
      fa.tflatMap[Tuple1[B]]((a: Tuple1[A]) => f(a._1).map(Tuple1.apply))
    override def xmap[A, B](fa: Segment[A], f: A => B, g: B => A): Segment[B] =
      fa.map(f)
  }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] = {
    new Url(joinDirectives(path.directive, qs.directive))
  }

  //***************
  // Query strings
  //***************

  implicit lazy val stringQueryString: QueryStringParam[String] =
    (name, map) => map.get(name).flatMap(vs => vs.headOption)

  def qs[A](name: String, docs: Documentation)(implicit param: QueryStringParam[A]): QueryString[A] =
    new QueryString[A](Directives.parameterMultiMap.flatMap { kvs =>
      param(name, kvs) match {
        case Some(a) => Directives.provide(a)
        case None    => malformedRequest
      }
    })

  implicit def optionalQueryStringParam[A](implicit param: QueryStringParam[A]): QueryStringParam[Option[A]] =
    (name, qs) =>
      qs.get(name) match {
        case None    => Some(None)
        case Some(_) => param(name, qs).map(Some(_))
      }

  implicit def repeatedQueryStringParam[A, CC[X] <: Iterable[X]](implicit param: QueryStringParam[A], factory: Factory[A, CC[A]]): QueryStringParam[CC[A]] =
    (name, qs) =>
      qs.get(name) match {
        case None     => Some(factory.newBuilder.result())
        case Some(vs) =>
          vs.foldLeft[Option[mutable.Builder[A, CC[A]]]](Some(factory.newBuilder)) {
            case (None, _) => None
            case (Some(b), v) =>
              // Pretend that this was the query string and delegate to the `A` query string param
              param(name, Map(name -> (v :: Nil))).map(b += _)
          }.map(_.result())
      }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] = {
    new QueryString(joinDirectives(first.directive, second.directive))
  }

  implicit lazy val urlPartialInvFunctor: PartialInvariantFunctor[Url] = new PartialInvariantFunctor[Url] {
    def xmapPartial[A, B](fa: Url[A], f: A => Option[B], g: B => A): Url[B] =
      new Url(fa.directive.flatMap { a =>
        f(a) match {
          case Some(b) => Directives.provide(b)
          case None    => malformedRequest
        }
      })
    override def xmap[A, B](fa: Url[A], f: A => B, g: B => A): Url[B] =
      new Url(fa.directive.map(f))
  }

  // ********
  // Paths
  // ********

  implicit def stringSegment: Segment[String] = Segment

  def segment[A](name: String, docs: Documentation)(implicit s: Segment[A]): Path[A] = {
    // If there is no segment, the path must not match
    // for instance, given the `path / foo / segment[Int]` definition,
    // an incoming request `"/foo"` or `"/foo/"` does not match,
    // whereas `"/foo/42"` matches and succeeds, and `"/foo/bar"` matches and fails.
    val directive: Directive1[A] =
      Directives.extract { ctx =>
        (Slash.? ~ PathEnd).apply(ctx.unmatchedPath)
      }.flatMap {
        case _: PathMatcher.Matched[_] => Directives.reject
        case PathMatcher.Unmatched => Directives.pathPrefix(s) | malformedRequest
      }
    new Path(directive)
  }

  def remainingSegments(name: String, docs: Documentation): Path[String] =
    new Path(Directives.path(PathMatchers.Remaining))

  def staticPathSegment(segment: String): Path[Unit] = {
    val directive = if(segment.isEmpty) // We cannot use Directives.pathPrefix("") because it consumes also a leading slash
      Directives.pass
    else
      Directives.pathPrefix(segment)
    new Path(convToDirective1(directive))
  }

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] = {
    new Path(joinDirectives(first.pathPrefix, second.pathPrefix))
  }


  /**
    * Simpler alternative to [[Directive.&()]] method
    */
  protected def joinDirectives[T1, T2](dir1: Directive1[T1], dir2: Directive1[T2])(implicit tupler: Tupler[T1, T2]): Directive1[tupler.Out] = {
    Directive[Tuple1[tupler.Out]] { inner =>
      dir1.tapply { case Tuple1(prefix) => dir2.tapply { case Tuple1(suffix) => inner(Tuple1(tupler(prefix, suffix))) } }
    }
  }

  protected def convToDirective1(directive: Directive0): Directive1[Unit] = {
    directive.tmap(_ => Tuple1(()))
  }

  implicit class Directive0Ops(val dir0: Directive0) {
    def dir1: Directive1[Unit] = convToDirective1(dir0)
  }

  // TODO Improve error reporting
  private[akkahttp] def malformedRequest: StandardRoute =
    Directives.complete(HttpResponse(StatusCodes.BadRequest))

}
