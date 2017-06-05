package endpoints.scalaj.client

import endpoints.Tupler
import endpoints.algebra.MuxRequest

import scala.concurrent.{ExecutionContext, Future}
import scalaj.http.{Http, HttpRequest, HttpResponse}

trait Endpoints extends endpoints.algebra.Endpoints {


  Http("").param


  type HttpRequestModifier[A] = (A, HttpRequest) => HttpRequest


  /** Information carried by requests’ headers */
  override type RequestHeaders[A] = A => Seq[(String, String)]

  /**
    * No particular information. Does not mean that the headers *have to*
    * be empty. Just that, from a server point of view no information will
    * be extracted from them, and from a client point of view no particular
    * headers will be built in the request.
    */
  override def emptyHeaders: RequestHeaders[Unit] = _ => Seq()

  /** Information carried by a whole request (headers and entity) */
  override type Request[A] = A => HttpRequest
  /** Information carried by request entity */
  override type RequestEntity[A] = HttpRequestModifier[A]

  /**
    * Empty request.
    */
  override def emptyRequest: RequestEntity[Unit] = x => x._2

  /**
    * Request for given parameters
    *
    * @param method  Request method
    * @param url     Request URL
    * @param entity  Request entity
    * @param headers Request headers
    */
  def request[U, E, H, UE](method: Method,
                           url: Url[U],
                           entity: RequestEntity[E] = emptyRequest,
                           headers: RequestHeaders[H] = emptyHeaders
                          )(implicit tuplerUE: Tupler.Aux[U, E, UE], tuplerUEH: Tupler[UE, H]): Request[tuplerUEH.Out] =
    (abc) => {
      val (ue, h) = tuplerUEH.unapply(abc)
      val (u, e) = tuplerUE.unapply(ue)
      val req = Http(url(url))
        .headers(headers(h))
      entity(e, req)
        .method(method)
    }


  /** Information carried by a response */
  override type Response[A] = HttpResponse[String] => A

  /**
    * Empty response.
    */
  override def emptyResponse: Response[Unit] =

  /**
    * Information carried by an HTTP endpoint
    *
    * @tparam Req Information carried by the request
    * @tparam Resp Information carried by the response
    */
  case class Endpoint[Req, Resp](request: Request[Req], response: Response[Resp]){

    def call(args: Req): Resp = response(request(args).asString)

    /**
      * This method just wraps a call in a Future and is not real async call
      */
    def callAsync(args: Req)(implicit ec: ExecutionContext): Future[Resp] =
      Future { call(args)}
  }

  /**
    * HTTP endpoint.
    *
    * @param request  Request
    * @param response Response
    */
  override def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] = {
    Endpoint(request, response)
  }

  /**
    * Information carried by a multiplexed HTTP endpoint.
    */
  override type MuxEndpoint[A,B, Transport] = Nothing

  override def muxEndpoint[Req <: MuxRequest, Resp, Transport](request: Request[Transport], response: Response[Transport]): MuxEndpoint[Req, Resp, Transport] =
    throw new UnsupportedOperationException("Not implemented")

  /** A query string carrying an `A` information */
  override type QueryString[A] = A => String

  /** Concatenates two `QueryString`s */
  override def combineQueryStrings[A, B](first: Endpoints.this.type, second: Endpoints.this.type)(implicit tupler: Tupler[A, B]): Endpoints.this.type = ???

  /**
    * Builds a `QueryString` with one parameter.
    *
    * @param name Parameter’s name
    * @tparam A Type of the value carried by the parameter
    */
  override def qs[A](name: String)(implicit value: Endpoints.this.type): QueryStringParam[A] = ???

  /**
    * Builds a `QueryString` with one optional parameter of type `A`.
    *
    * @param name Parameter’s name
    */
  override def optQs[A](name: String)(implicit value: Endpoints.this.type): Endpoints.this.type = ???

  /**
    * A single query string parameter carrying an `A` information.
    */
  override type QueryStringParam[A] = this.type

  /** Ability to define `String` query string parameters */
  override implicit def stringQueryString: Endpoints.this.type = ???

  /** Ability to define `Int` query string parameters */
  override implicit def intQueryString: Endpoints.this.type = ???

  /** Query string parameter containing a `Long` value */
  override implicit def longQueryString: Endpoints.this.type = ???

  /**
    * An URL path segment carrying an `A` information.
    */
  override type Segment = this.type

  /** Ability to define `String` path segments */
  override implicit def stringSegment: Endpoints.this.type = ???

  /** Ability to define `Int` path segments */
  override implicit def intSegment: Endpoints.this.type = ???

  /** Segment containing a `Long` value */
  override implicit def longSegment: Endpoints.this.type = ???

  /** An URL path carrying an `A` information */
  override type Path = this.type

  /** Builds a static path segment */
  override def staticPathSegment(segment: String): Endpoints.this.type = ???

  /** Builds a path segment carrying an `A` information */
  override def segment[A](implicit s: Endpoints.this.type): Endpoints.this.type = ???

  /** Chains the two paths */
  override def chainPaths[A, B](first: Endpoints.this.type, second: Endpoints.this.type)(implicit tupler: Tupler[A, B]): Endpoints.this.type = ???

  /**
    * An URL carrying an `A` information
    */
  override type Url[A] = A => String

  /** Builds an URL from the given path and query string */
  override def urlWithQueryString[A, B](path: Endpoints.this.type, qs: Endpoints.this.type)(implicit tupler: Tupler[A, B]): Endpoints.this.type = ???

  override type Method = String

  override def Get: Method = "GET"

  override def Post: Method = "POST"

  override def Put: Method = "PUT"

  override def Delete: Method = "DELETE"
}
