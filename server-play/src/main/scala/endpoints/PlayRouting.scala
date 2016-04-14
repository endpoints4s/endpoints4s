package endpoints

import java.net.URLDecoder

import io.circe.{Decoder, Encoder, Json, jawn}
import play.api.http.Writeable
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.streams.Accumulator
import play.api.mvc._

trait PlayRouting extends Endpoints {

  trait Path[A] extends PathOps[A] {
    def decode(segments: List[String]): Option[(A, List[String])]
    def encode(a: A): String
  }

  def static(segment: String): Path[Unit] =
    new Path[Unit] {
      def decode(segments: List[String]): Option[(Unit, List[String])] =
        segments match {
          case s :: ss if s == segment => Some(((), ss))
          case _ => None
        }
      def encode(unit: Unit): String = segment
    }

  def dynamic: Path[String] =
    new Path[String] {
      def decode(segments: List[String]): Option[(String, List[String])] =
        segments match {
          case s :: ss => Some((s, ss))
          case Nil => None
        }

      def encode(s: String) = s
    }

  def chained[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out] =
    new Path[fc.Out] {
      def decode(segments: List[String]) =
        for {
          (a, segments2) <- first.decode(segments)
          (b, segments3) <- second.decode(segments2)
        } yield (fc(a, b), segments3)
      def encode(ab: fc.Out) = {
        val (a, b) = fc.unapply(ab)
        first.encode(a) ++ "/" ++ second.encode(b)
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
        .map(s => URLDecoder.decode(s, "utf-8"))
    path.decode(segments).collect { case (a, Nil) => a }
  }


  def get[A](path: Path[A]): Request[A] =
    new Request[A] {
      def decode(requestHeader: RequestHeader) =
        if (requestHeader.method == "GET") {
          extractFromPath(path, requestHeader).map(a => BodyParser(_ => Accumulator.done(Right(a))))
        } else None
      def encode(a: A) = Call("GET", path.encode(a))
    }

  def post[A, B](path: Path[A], entity: RequestEntity[B])(implicit fc: FlatConcat[A, B]): Request[fc.Out] =
    new Request[fc.Out] {
      def decode(requestHeader: RequestHeader) =
        if (requestHeader.method == "POST") {
          extractFromPath(path, requestHeader).map(a => entity.map(b => fc.apply(a, b)))
        } else None
      def encode(ab: fc.Out) = {
        val (a, _) = fc.unapply(ab)
        Call("POST", path.encode(a))
      }
    }

  def jsonRequest[A : JsonRequest] =
    BodyParsers.parse.raw.validate { buffer =>
      jawn.parseFile(buffer.asFile)
        .flatMap(Decoder[A].decodeJson).toEither
        .left.map(error => Results.BadRequest)
    }


  type Response[A] = A => Result

  def jsonResponse[A](implicit encoder: Encoder[A]) = a => Results.Ok(encoder.apply(a))


  case class Endpoint[A, B](request: Request[A], response: Response[B]) {
    def call(a: A): Call = request.encode(a)
    def withService(service: A => B): EndpointWithHandler[A, B] = EndpointWithHandler(this, service)
  }

  case class EndpointWithHandler[A, B](endpoint: Endpoint[A, B], service: A => B) {
    def playHandler(header: RequestHeader): Option[Handler] =
      endpoint.request.decode(header)
        .map(a => Action(a)(request => endpoint.response(service(request.body))))
  }

  type JsonRequest[A] = Decoder[A]

  type JsonResponse[A] = Encoder[A]

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

  implicit def writeableJson(implicit codec: Codec): Writeable[Json] =
    new Writeable[Json](json => codec.encode(json.noSpaces), Some("application/json"))

}
