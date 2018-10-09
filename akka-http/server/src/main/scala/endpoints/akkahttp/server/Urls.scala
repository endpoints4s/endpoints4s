package endpoints.akkahttp.server

import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, PredefinedFromStringUnmarshallers, Unmarshaller}
import endpoints.algebra.Documentation
import endpoints.{InvariantFunctor, Tupler, algebra}

/**
  * [[algebra.Urls]] interpreter that decodes and encodes URLs.
  *
  * @group interpreters
  */
trait Urls extends algebra.Urls {

  import akka.http.scaladsl.server.Directives._

  class Path[T](override val directive: Directive1[T]) extends Url[T](directive)

  class Url[T](val directive: Directive1[T])

  class QueryString[T](val directive: Directive1[T])

  type QueryStringParam[T] = FromStringUnmarshaller[T]

  type Segment[T] = PathMatcher1[T]


  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] = {
    new Url(joinDirectives(path.directive, qs.directive))
  }

  //***************
  // Query strings
  //***************

  implicit def intQueryString: QueryStringParam[Int] = PredefinedFromStringUnmarshallers.intFromStringUnmarshaller

  implicit def stringQueryString: QueryStringParam[String] = Unmarshaller.identityUnmarshaller[String]

  implicit def longQueryString: QueryStringParam[Long] = PredefinedFromStringUnmarshallers.longFromStringUnmarshaller

  def qs[A](name: String, docs: Documentation)(implicit value: QueryStringParam[A]): QueryString[A] = {
    new QueryString[A](Directives.parameter(name.as[A]))
  }

  def optQs[A](name: String, docs: Documentation)(implicit value: QueryStringParam[A]): QueryString[Option[A]] = {
    new QueryString[Option[A]](Directives.parameter(name.as[A].?))
  }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] = {
    new QueryString(joinDirectives(first.directive, second.directive))
  }

  implicit lazy val urlInvFunctor: InvariantFunctor[Url] = new InvariantFunctor[Url] {
    override def xmap[From, To](f: Url[From], map: From => To, contramap: To => From): Url[To] =
      new Url(f.directive.map(map))
  }

  // ********
  // Paths
  // ********

  implicit def intSegment: Segment[Int] = IntNumber

  implicit def stringSegment: Segment[String] = Segment

  implicit def longSegment: Segment[Long] = LongNumber

  def segment[A](name: String, docs: Documentation)(implicit s: Segment[A]): Path[A] = {
    new Path(Directives.pathPrefix(s))
  }

  def staticPathSegment(segment: String): Path[Unit] = {
    val directive = if(segment.isEmpty) // We cannot use Directives.pathPrefix("") because it consumes also a leading slash
      Directives.pass
    else
      Directives.pathPrefix(segment)
    new Path(convToDirective1(directive))
  }

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] = {
    new Path(joinDirectives(first.directive, second.directive))
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

}
