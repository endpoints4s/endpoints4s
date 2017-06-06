package endpoints.scalaj.client

import endpoints.Tupler
import endpoints.algebra.MuxRequest

import scala.concurrent.{ExecutionContext, Future}
import scalaj.http.{Http, HttpRequest, HttpResponse}

trait Endpoints extends endpoints.algebra.Endpoints {


  override type RequestHeaders[A] = A => Seq[(String, String)]
  override type Request[A] = A => HttpRequest
  override type RequestEntity[A] = ((A, HttpRequest)) => HttpRequest
  override type Response[A] = HttpResponse[String] => A
  override type MuxEndpoint[A, B, Transport] = Nothing
  case class QueryString[A](f: A => Seq[(String, String)]) extends QueryStringOps[A]
  override type QueryStringParam[A] = A => String
  override type Segment[A] = A => String
  case class Path[A](toStr: A => String) extends Url(toStr.andThen(Http(_))) with PathOps[A]
  class Url[A](val toReq: A => HttpRequest)
  override type Method = String

  override def emptyHeaders: RequestHeaders[Unit] = _ => Seq()

  override def emptyRequest: RequestEntity[Unit] =  (x: (Unit, HttpRequest)) => x._2

  def request[U, E, H, UE](method: Method,
                           url: Url[U],
                           entity: RequestEntity[E] = emptyRequest,
                           headers: RequestHeaders[H] = emptyHeaders
                          )(implicit tuplerUE: Tupler.Aux[U, E, UE], tuplerUEH: Tupler[UE, H]): Request[tuplerUEH.Out] =
    (abc) => {
      val (ue, h) = tuplerUEH.unapply(abc)
      val (u, e) = tuplerUE.unapply(ue)
      val req = url.toReq(u)
        .headers(headers(h))
      entity((e, req))
        .method(method)
    }

  override def emptyResponse: Response[Unit] = x => {
    x.throwServerError
    ()
  }

  override def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] = {
    Endpoint(request, response)
  }

  override def muxEndpoint[Req <: MuxRequest, Resp, Transport](request: Request[Transport], response: Response[Transport]): MuxEndpoint[Req, Resp, Transport] =
    throw new UnsupportedOperationException("Not implemented")

  override implicit def stringQueryString: QueryStringParam[String] = identity

  override implicit def intQueryString: QueryStringParam[Int] = _.toString

  override implicit def longQueryString: QueryStringParam[Long] = _.toString

  override def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] = {
    QueryString(ab => {
      val (a, b) = tupler.unapply(ab)
      first.f(a) ++ second.f(b)
    })
  }

  override implicit def stringSegment: Segment[String] = identity

  override implicit def intSegment: Segment[Int] = _.toString

  override implicit def longSegment: Segment[Long] = _.toString


  override def qs[A](name: String)(implicit value: QueryStringParam[A]): QueryString[A] =
    QueryString(a => Seq((name, value(a))))

  def optQs[A](name: String)(implicit value: QueryStringParam[A]): QueryString[Option[A]] =
    QueryString(a => a.map(x => Seq((name, value(x)))).getOrElse(Seq()))

  override def staticPathSegment(segment: String): Path[Unit] = Path(_ => segment)

  def segment[A](implicit s: Segment[A]): Path[A] = Path(s)

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] = {
    Path(ab => {
      val (a,b) = tupler.unapply(ab)
      first.toStr(a) + "/" + second.toStr(b)
    })
  }

  /** Builds an URL from the given path and query string */
  override def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] = {
    new Url(ab => {
      val (a, b) = tupler.unapply(ab)
      path.toReq(a)
        .params(qs.f(b))
    })
  }

  override def Get: Method = "GET"

  override def Post: Method = "POST"

  override def Put: Method = "PUT"

  override def Delete: Method = "DELETE"

  /**
    * Information carried by an HTTP endpoint
    *
    * @tparam Req  Information carried by the request
    * @tparam Resp Information carried by the response
    */
  case class Endpoint[Req, Resp](request: Request[Req], response: Response[Resp]) {

    /**
      * This method just wraps a call in a Future and is not real async call
      */
    def callAsync(args: Req)(implicit ec: ExecutionContext): Future[Resp] =
      Future {
        call(args)
      }

    def call(args: Req): Resp = response(request(args).asString)
  }

}
