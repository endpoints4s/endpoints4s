package endpoints.akkahttp

import akka.http.scaladsl.server.util.{Tuple, Tupler => AkkaTupler}
import akka.http.scaladsl.server.{Directive, Directives, PathMatcher1}
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import endpoints.{Tupler, algebra}

import scala.language.higherKinds

/**
  * [[algebra.Urls]] interpreter that decodes and encodes URLs.
  */
class Urls extends algebra.Urls {

  import akka.http.scaladsl.server.Directives._


  class Path[T](override val directive: Directive[T]) extends Url[T](directive) with PathOps[T]

  class Url[T](val directive: Directive[T])

  class QueryString[T](val directive: Directive[T]) extends QueryStringOps[T]

  override type QueryStringParam[T] = FromStringUnmarshaller[T]

  override type Segment[T] = PathMatcher1[T]

  override def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] = {
    implicit def akkaHttpTuple: Tuple[tupler.Out] = null
    new Url(joinDirectives(path.directive, qs.directive))
  }

  override def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] = {
    new Path(joinDirectives(first.directive, second.directive))
  }

  override def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] = {
    new QueryString(joinDirectives(first.directive, second.directive))
  }

  override implicit def intQueryString: QueryStringParam[Int] = implicitly[Unmarshaller[String, Int]]

  override def qs[A](name: String)(implicit value: QueryStringParam[A]): QueryString[A] = {
    // it is single value, so we don't want it to be put in Tuple1
    implicit val tuple: Tuple[A] = Tuple.yes
    val directive = Directives.parameter(name.as[A]).tmap(t => t._1)
    new QueryString[A](directive)
  }

  override implicit def intSegment: Segment[Int] = IntNumber

  override def segment[A](implicit s: Segment[A]): Path[A] = {
    val directive = Directives.pathPrefix(s)
    new Path(extractFromTuple(directive))
  }

  override def staticPathSegment(segment: String): Path[Unit] = {
    new Path(Directives.pathPrefix(segment))
  }

  override val path: Path[Unit] = new Path(Directives.pass)

  override implicit def stringQueryString: QueryStringParam[String] = implicitly[Unmarshaller[String, String]]

  override implicit def stringSegment: Segment[String] = Segment


  /**
    * Simpler alternative to [[Directive.&()]] method
    */
  private def joinDirectives[T1, T2](dir1: Directive[T1], dir2: Directive[T2])(implicit tupler: Tupler[T1, T2]): Directive[tupler.Out] = {
    Directive[tupler.Out] { inner =>
      dir1.tapply { prefix => dir2.tapply { suffix => inner(tupler(prefix, suffix)) } }
    }(Tuple.yes)
  }

  /**
    * We are breaking [[Directive]] by putting inside something that is probably not tuple, so we can keep methods signatures
    * Alternatively we can keep everything as Directive1
    */
  private def extractFromTuple[T](directive: Directive[Tuple1[T]]): Directive[T] = {
    directive.tmap(t => t._1)(AkkaTupler.forTuple(Tuple.yes))
  }


}
