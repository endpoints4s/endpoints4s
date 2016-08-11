package endpoints

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets.UTF_8

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.{Applicative, Functor}
import play.api.libs.functional.syntax._
import play.api.libs.streams.Accumulator
import play.api.mvc.{Action, BodyParser, Call, Handler, RequestHeader, Result, Results}
import play.twirl.api.Html

import scala.language.higherKinds
import scala.util.Try

trait PlayRouting extends EndpointsAlg {

  val utf8Name = UTF_8.name()

  // No Kleisli in play-functional…
  type RequestExtractor[A] = RequestHeader => Option[A]

  // FIXME Pull this algebra up to EndpointsAlg
  implicit lazy val functorRequestExtractor: Functor[RequestExtractor] =
    new Functor[RequestExtractor] {
      def fmap[A, B](m: RequestExtractor[A], f: A => B): RequestExtractor[B] =
        request => m(request).map(f)
    }

  implicit lazy val applicativeRequestExtractor: Applicative[RequestExtractor] =
    new Applicative[RequestExtractor] {
      def apply[A, B](mf: RequestExtractor[A => B], ma: RequestExtractor[A]): RequestExtractor[B] =
        request => mf(request).flatMap(f => ma(request).map(f))
      def pure[A](a: A): RequestExtractor[A] =
        _ => Some(a)
      def map[A, B](m: RequestExtractor[A], f: A => B): RequestExtractor[B] =
        request => m(request).map(f)
    }

  // Temporary?
  implicit final class ApplicativeMapSyntax[F[_], A](fa: F[A]) {
    @inline def map[B](f: A => B)(implicit applicative: Applicative[F]): F[B] = applicative.map(fa, f)
  }

  trait Segment[A] {
    def decode(segment: String): Option[A]
    def encode(a: A): String
  }

  implicit def stringSegment: Segment[String] =
    new Segment[String] {
      def decode(segment: String) = Some(segment)
      def encode(s: String) = URLEncoder.encode(s, utf8Name)
    }

  implicit def intSegment: Segment[Int] =
    new Segment[Int] {
      def decode(segment: String) = Try(segment.toInt).toOption
      def encode(a: Int) = a.toString
    }


  trait QueryString[A] extends QueryStringOps[A] {
    def decode(qs: Map[String, Seq[String]]): Option[A]
    def encode(a: A): Map[String, Seq[String]] // FIXME Encode to a String for better performance
  }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
    new QueryString[tupler.Out] {
      def decode(qs: Map[String, Seq[String]]) =
        for {
          a <- first.decode(qs)
          b <- second.decode(qs)
        } yield tupler(a, b)
      def encode(ab: tupler.Out) = {
        val tupler(a, b) = ab
        first.encode(a) ++ second.encode(b)
      }
    }

  def qs[A](name: String)(implicit value: QueryStringValue[A]) =
    new QueryString[A] {
      def decode(qs: Map[String, Seq[String]]) = value.decode(name, qs)
      def encode(a: A) = value.encode(name, a)
    }

  trait QueryStringValue[A] {
    def decode(name: String, qs: Map[String, Seq[String]]): Option[A]
    def encode(name: String, a: A): Map[String, Seq[String]]
  }

  implicit def stringQueryString: QueryStringValue[String] =
    new QueryStringValue[String] {
      def decode(name: String, qs: Map[String, Seq[String]]) =
        qs.get(name).flatMap(_.headOption)
      def encode(name: String, s: String) =
        Map(name -> Seq(URLEncoder.encode(s, utf8Name)))
    }

  implicit def intQueryString: QueryStringValue[Int] =
    new QueryStringValue[Int] {
      def decode(name: String, qs: Map[String, Seq[String]]) =
        qs.get(name).flatMap(_.headOption).flatMap(s => Try(s.toInt).toOption)
      def encode(name: String, i: Int) =
        Map(name -> Seq(i.toString))
    }


  trait Path[A] extends PathOps[A] with Url[A] {
    def decode(segments: List[String]): Option[(A, List[String])]
    def encode(a: A): String

    final val decodeUrl = pathExtractor(this)
    final def encodeUrl(a: A) = encode(a)
  }

  def staticPathSegment(segment: String): Path[Unit] =
    new Path[Unit] {
      def decode(segments: List[String]): Option[(Unit, List[String])] =
        segments match {
          case s :: ss if s == segment => Some(((), ss))
          case _ => None
        }
      def encode(unit: Unit): String = segment
    }

  def segment[A](implicit A: Segment[A]): Path[A] =
    new Path[A] {
      def decode(segments: List[String]) = {
        def uncons[B](bs: List[B]): Option[(B, List[B])] =
          bs match {
            case head :: tail => Some((head, tail))
            case Nil => None
          }
        for {
          (s, ss) <- uncons(segments)
          a <- A.decode(s)
        } yield (a, ss)
      }
      def encode(a: A) = A.encode(a)
    }

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    new Path[tupler.Out] {
      def decode(segments: List[String]) =
        for {
          (a, segments2) <- first.decode(segments)
          (b, segments3) <- second.decode(segments2)
        } yield (tupler(a, b), segments3)
      def encode(ab: tupler.Out) = {
        val tupler(a, b) = ab
        first.encode(a) ++ "/" ++ second.encode(b)
      }
    }

  trait Url[A] {
    def decodeUrl: RequestExtractor[A]
    def encodeUrl(a: A): String
  }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] =
    new Url[tupler.Out] {

      val decodeUrl: RequestExtractor[tupler.Out] =
        pathExtractor(path)
          .and(queryStringExtractor(qs))
          .apply((a, b) => tupler(a, b))

      def encodeUrl(ab: tupler.Out) = {
        val tupler(a, b) = ab
        val encodedQs =
          qs.encode(b)
            .flatMap { case (n, vs) => vs.map(v => (n, v)) }
            .map { case (n, v) => s"$n=$v" }
            .mkString("&")
        s"${path.encode(a)}?$encodedQs"
      }
    }

  private def pathExtractor[A](path: Path[A]): RequestExtractor[A] =
    request => {
      val segments =
        request.path
          .split("/").to[List]
          .map(s => URLDecoder.decode(s, utf8Name))
      path.decode(if (segments.isEmpty) List("") else segments).collect { case (a, Nil) => a }
    }

  private def queryStringExtractor[A](qs: QueryString[A]): RequestExtractor[A] =
    request => qs.decode(request.queryString)


  type Headers[A] = RequestExtractor[A] // FIXME Use a more precise type (like play.api.mvc.Headers => Option[A])

  lazy val emptyHeaders: Headers[Unit] = _ => Some(())


  trait Request[A] {
    def decode: RequestExtractor[BodyParser[A]]
    def encode(a: A): Call
  }

  type RequestEntity[A] = BodyParser[A]

  private def extractMethod(method: String): RequestExtractor[Unit] =
    request =>
      if (request.method == method) Some(())
      else None

  private def extractMethodUrlAndHeaders[A, B](method: String, url: Url[A], headers: Headers[B]): RequestExtractor[(A, B)] =
    extractMethod(method)
      .andKeep(url.decodeUrl)
      .and(headers)
      .tupled


  def get[A, B](url: Url[A], headers: Headers[B])(implicit tupler: Tupler[A, B]): Request[tupler.Out] =
    new Request[tupler.Out] {
      val decode =
        extractMethodUrlAndHeaders("GET", url, headers)
          .map { case (a, b) => BodyParser(_ => Accumulator.done(Right(tupler(a, b)))) }
      def encode(ab: tupler.Out) = {
        val tupler(a, _) = ab
        Call("GET", url.encodeUrl(a))
      }
    }

  def post[A, B, C, AB](url: Url[A], entity: RequestEntity[B], headers: Headers[C])(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
    new Request[tuplerABC.Out] {
      val decode =
        extractMethodUrlAndHeaders("POST", url, headers)
          .map { case (a, c) => entity.map(b => tuplerABC(tuplerAB(a, b), c)) }
      def encode(abc: tuplerABC.Out) = {
        val tuplerABC(tuplerAB(a, _), _) = abc
        Call("POST", url.encodeUrl(a))
      }
    }


  type Response[A] = A => Result

  val emptyResponse: Response[Unit] = _ => Results.Ok

  val htmlResponse: Response[Html] = html => Results.Ok(html)



  case class Endpoint[A, B](request: Request[A], response: Response[B]) {
    def call(a: A): Call = request.encode(a)
    def implementedBy(service: A => B): EndpointWithHandler[A, B] = EndpointWithHandler(this, service andThen Future.successful)
    def implementedByAsync(service: A => Future[B]): EndpointWithHandler[A, B] = EndpointWithHandler(this, service)
  }

  case class EndpointWithHandler[A, B](endpoint: Endpoint[A, B], service: A => Future[B]) {
    def playHandler(header: RequestHeader): Option[Handler] =
      endpoint.request.decode(header)
        .map(a => Action.async(a){ request =>
          service(request.body).map { b =>
            endpoint.response(b)
          }
        })
  }

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    Endpoint(request, response)


  def routesFromEndpoints(endpoints: EndpointWithHandler[_, _]*): PartialFunction[RequestHeader, Handler] =
    Function.unlift { request : RequestHeader =>
      def loop(es: Seq[EndpointWithHandler[_, _]]): Option[Handler] =
        es match {
          case e +: es2 => e.playHandler(request).orElse(loop(es2))
          case Nil => None
        }
      loop(endpoints)
    }

}
