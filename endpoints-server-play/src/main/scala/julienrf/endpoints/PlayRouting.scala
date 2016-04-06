package julienrf.endpoints

import java.net.URLDecoder

import io.circe.{Decoder, Encoder, Json, jawn}
import play.api.http.Writeable
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Done
import play.api.mvc.{BodyParsers, BodyParser, Action, Codec, Handler, RequestHeader, Result, Results}

trait PlayRouting extends Endpoints {

  type Path[A] = List[String] => Option[(A, List[String])]

  def static(segment: String) = {
    case s :: ss if s == segment => Some(((), ss))
    case _ => None
  }

  def dynamic: List[String] => Option[(String, List[String])] = {
    case s :: ss => Some((s, ss))
    case Nil => None
  }

  def chained[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out] =
    ss =>
      for {
        (a, ss2) <- first(ss)
        (b, ss3) <- second(ss2)
      } yield (fc(a, b), ss3)


  type Request[A] = RequestHeader => Option[BodyParser[A]]

  type RequestEntity[A] = BodyParser[A]

  private def extractFromPath[A](path: Path[A], request: RequestHeader): Option[A] = {
    val segments =
      if (request.path == "" || request.path == "/") Nil
      else
        request.path
          .drop(1)
          .split("/").to[List]
          .map(s => URLDecoder.decode(s, "utf-8"))
    path(segments).collect { case (a, Nil) => a }
  }


  def get[A](path: Path[A]) =
    request =>
      if (request.method == "GET") {
        extractFromPath(path, request).map(a => BodyParser(_ => Done(Right(a))))
      } else None

  def post[A, B](path: Path[A], entity: RequestEntity[B])(implicit fc: FlatConcat[A, B]): Request[fc.Out] =
    request =>
      if (request.method == "POST") {
        extractFromPath(path, request).map(a => entity.map(b => fc.apply(a, b)))
      } else None

  object request extends RequestApi {
    def jsonEntity[A : RequestMarshaller] =
      BodyParsers.parse.raw.validate { buffer =>
        jawn.parseFile(buffer.asFile)
          .flatMap(Decoder[A].decodeJson).toEither
          .left.map(error => Results.BadRequest)
      }
  }


  type Response[A] = A => Result

  def jsonEntity[A](implicit encoder: Encoder[A]) = a => Results.Ok(encoder.apply(a))


  case class Endpoint[A, B](request: Request[A], response: Response[B]) {
    def withService(service: A => B): EndpointWithHandler[A, B] = EndpointWithHandler(this, service)
  }

  case class EndpointWithHandler[A, B](endpoint: Endpoint[A, B], service: A => B) {
    def playHandler(header: RequestHeader): Option[Handler] =
      endpoint.request(header)
        .map(a => Action(a)(request => endpoint.response(service(request.body))))
  }

  type RequestMarshaller[A] = Decoder[A]

  type ResponseMarshaller[A] = Encoder[A]

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
