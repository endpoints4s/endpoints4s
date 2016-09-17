package endpoints

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.InvariantFunctor
import play.api.libs.functional.syntax._
import play.api.libs.streams.Accumulator
import play.api.mvc.{Action, BodyParser, Call, Handler, RequestHeader, Result, Results}
import play.twirl.api.Html

import scala.language.higherKinds

trait EndpointPlayRouting extends EndpointAlg with UrlPlayRouting {

  type Headers[A] = RequestExtractor[A] // FIXME Use a more precise type (like play.api.mvc.Headers => Option[A])

  lazy val emptyHeaders: Headers[Unit] = _ => Some(())


  trait Request[A] {
    def decode: RequestExtractor[BodyParser[A]]
    def encode(a: A): Call
  }

  implicit lazy val invariantFunctorRequest: InvariantFunctor[Request] =
    new InvariantFunctor[Request] {
      def inmap[A, B](m: Request[A], f1: A => B, f2: B => A): Request[B] =
        new Request[B] {
          def decode: RequestExtractor[BodyParser[B]] =
            functorRequestExtractor.fmap(m.decode, (bodyParser: BodyParser[A]) => bodyParser.map(f1))
          def encode(a: B): Call = m.encode(f2(a))
        }
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
          .map { case (a, b) => BodyParser(_ => Accumulator.done(Right(tupler.apply(a, b)))) }
      def encode(ab: tupler.Out) = Call("GET", url.encodeUrl(tupler.unapply(ab)._1))
    }

  def post[A, B, C, AB](url: Url[A], entity: RequestEntity[B], headers: Headers[C])(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
    new Request[tuplerABC.Out] {
      val decode =
        extractMethodUrlAndHeaders("POST", url, headers)
          .map { case (a, c) => entity.map(b => tuplerABC.apply(tuplerAB.apply(a, b), c)) }
      def encode(abc: tuplerABC.Out) = {
        val (ab, c) = tuplerABC.unapply(abc)
        val (a, b) = tuplerAB.unapply(ab)
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
