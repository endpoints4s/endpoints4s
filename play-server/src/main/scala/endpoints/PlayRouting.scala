package endpoints

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets.UTF_8

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.streams.Accumulator
import play.api.mvc.{Action, BodyParser, Call, Handler, RequestHeader, Result, Results}
import play.twirl.api.Html

import scala.util.Try

trait PlayRouting extends EndpointsAlg {

  val utf8Name = UTF_8.name()

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
        val (a, b) = tupler.unapply(ab)
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

    final def decodeUrl(requestHeader: RequestHeader) = extractFromPath(this, requestHeader)
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
        val (a, b) = tupler.unapply(ab)
        first.encode(a) ++ "/" ++ second.encode(b)
      }
    }

  trait Url[A] {
    def decodeUrl(requestHeader: RequestHeader): Option[A]
    def encodeUrl(a: A): String
  }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] =
    new Url[tupler.Out] {
      def decodeUrl(requestHeader: RequestHeader) =
        for {
          a <- extractFromPath(path, requestHeader)
          b <- qs.decode(requestHeader.queryString)
        } yield tupler(a, b)
      def encodeUrl(ab: tupler.Out) = {
        val (a, b) = tupler.unapply(ab)
        val encodedQs =
          qs.encode(b)
            .flatMap { case (n, vs) => vs.map(v => (n, v)) }
            .map { case (n, v) => s"$n=$v" }
            .mkString("&")
        s"${path.encode(a)}?$encodedQs"
      }
    }


  trait Request[A] {
    def decode(header: RequestHeader): Option[BodyParser[A]]
    def encode(a: A): Call
  }

  type RequestEntity[A] = BodyParser[A]

  private def extractFromPath[A](path: Path[A], request: RequestHeader): Option[A] = {
    val segments =
      request.path
        .split("/").to[List]
        .map(s => URLDecoder.decode(s, utf8Name))
    path.decode(if (segments.isEmpty) List("") else segments).collect { case (a, Nil) => a }
  }


  def get[A](url: Url[A]): Request[A] =
    new Request[A] {
      def decode(requestHeader: RequestHeader) =
        if (requestHeader.method == "GET") {
          url.decodeUrl(requestHeader).map(a => BodyParser(_ => Accumulator.done(Right(a))))
        } else None
      def encode(a: A) = Call("GET", url.encodeUrl(a))
    }

  def post[A, B](url: Url[A], entity: RequestEntity[B])(implicit tupler: Tupler[A, B]): Request[tupler.Out] =
    new Request[tupler.Out] {
      def decode(requestHeader: RequestHeader) =
        if (requestHeader.method == "POST") {
          url.decodeUrl(requestHeader).map(a => entity.map(b => tupler.apply(a, b)))
        } else None
      def encode(ab: tupler.Out) = {
        val (a, _) = tupler.unapply(ab)
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
