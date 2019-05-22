package endpoints.sttp.client

import java.net.URI

import endpoints.{InvariantFunctor, Semigroupal, Tupler, algebra}
import endpoints.algebra.Documentation
import com.softwaremill.sttp
import com.softwaremill.sttp.ResponseAs

import scala.language.higherKinds

/**
  * An interpreter for [[endpoints.algebra.Endpoints]] that builds a client issuing requests using
  * a sttp’s `com.softwaremill.sttp.SttpBackend`.
  *
  * Doest not support streaming responses for now
  *
  * @param host    Base of the URL of the service that implements the endpoints (e.g. "http://foo.com")
  * @param backend The underlying backend to use
  * @tparam R The monad wrapping the response. It is defined by the backend
  *
  * @group interpreters
  */
class Endpoints[R[_]](host: String, val backend: sttp.SttpBackend[R, Nothing]) extends algebra.Endpoints with Urls with Methods with StatusCodes {

  type SttpRequest = sttp.Request[_, Nothing]

  /**
    * A function that, given an `A` and a request model, returns an updated request
    * containing additional headers
    */
  type RequestHeaders[A] = (A, SttpRequest) => SttpRequest

  /** Does not modify the request */
  lazy val emptyHeaders: RequestHeaders[Unit] = (_, request) => request

  def header(name: String, docs: Documentation): RequestHeaders[String] = (value, request) => request.header(name, value)

  def optHeader(name: String, docs: Documentation): (Option[String], SttpRequest) => SttpRequest = {
    case (Some(value), request) => request.header(name, value)
    case (None, request) => request
  }

  implicit lazy val reqHeadersInvFunctor: InvariantFunctor[RequestHeaders] = new InvariantFunctor[RequestHeaders] {
    override def xmap[From, To](f: (From, SttpRequest) => SttpRequest, map: From => To, contramap: To => From): (To, SttpRequest) => SttpRequest =
      (to, request) => f(contramap(to), request)
  }

  implicit lazy val reqHeadersSemigroupal: Semigroupal[RequestHeaders] = new Semigroupal[RequestHeaders] {
    override def product[A, B](fa: (A, SttpRequest) => SttpRequest, fb: (B, SttpRequest) => SttpRequest)(implicit tupler: Tupler[A, B]): (tupler.Out, SttpRequest) => SttpRequest =
      (ab, request) => {
        val (a, b) = tupler.unapply(ab)
        fa(a, fb(b, request))
      }
  }


  /**
    * A function that takes an `A` information and returns a `sttp.Request`
    */
  type Request[A] = A => SttpRequest

  /**
    * A function that, given an `A` information and a `sttp.Request`, eventually returns a `sttp.Request`
    */
  type RequestEntity[A] = (A, SttpRequest) => SttpRequest

  lazy val emptyRequest: RequestEntity[Unit] = {
    case (_, req) => req
  }

  lazy val textRequest: RequestEntity[String] = {
    case (bodyValue, request) => request.body(bodyValue)
  }

  implicit def reqEntityInvFunctor: InvariantFunctor[RequestEntity] = new InvariantFunctor[RequestEntity] {
    override def xmap[From, To](f: (From, SttpRequest) => SttpRequest, map: From => To, contramap: To => From): (To, SttpRequest) => SttpRequest =
      (to, req) => f(contramap(to), req)
  }

  def request[A, B, C, AB, Out](
    method: Method, url: Url[A],
    entity: RequestEntity[B], docs: Documentation, headers: RequestHeaders[C]
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler.Aux[AB, C, Out]): Request[Out] =
    (abc: Out) => {
      val (ab, c) = tuplerABC.unapply(abc)
      val (a, b) = tuplerAB.unapply(ab)

      val uri: sttp.Id[sttp.Uri] = sttp.Uri(new URI(s"${host}${url.encode(a)}"))
      val sttpRequest: SttpRequest = method(sttp.sttp.get(uri = uri))
      entity(b, headers(c, sttpRequest))
    }

  /**
    * Trait that indicates how a response should be interpreted
    */
  trait SttpResponse[A] {
    /**
      * The type of the received body from the server
      */
    type ReceivedBody

    /**
      * To read the response body
      */
    def responseAs: sttp.ResponseAs[ReceivedBody, Nothing]

    def decodeEntity(response: sttp.Response[ReceivedBody]): R[A]
  }

  trait Response[A] {
    val entity: ResponseEntity[A]
    /**
      * Function to validate the response (headers, code).
      */
    def decodeResponse(response: sttp.Response[entity.ReceivedBody]): R[A]
  }

  type ResponseEntity[A] = SttpResponse[A]

  private[sttp] def mapResponseEntity[A, B](entity: ResponseEntity[A])(f: A => B): ResponseEntity[B] { type ReceivedBody = entity.ReceivedBody } =
    new ResponseEntity[B] {
      type ReceivedBody = entity.ReceivedBody
      def responseAs: ResponseAs[ReceivedBody, Nothing] = entity.responseAs
      def decodeEntity(response: sttp.Response[ReceivedBody]): R[B] =
        backend.responseMonad.map(entity.decodeEntity(response))(f)
    }

  /** Successfully decodes no information from a response */
  def emptyResponse: ResponseEntity[Unit] = new SttpResponse[Unit] {
    type ReceivedBody = Unit
    def responseAs = sttp.ignore
    def decodeEntity(response: sttp.Response[Unit]) = backend.responseMonad.unit(response.unsafeBody)
  }

  /** Successfully decodes string information from a response */
  def textResponse: ResponseEntity[String] = new SttpResponse[String] {
    type ReceivedBody = String
    def responseAs = sttp.asString
    def decodeEntity(response: sttp.Response[String]): R[String] = backend.responseMonad.unit(response.unsafeBody)
  }

  def response[A](statusCode: StatusCode, entity: ResponseEntity[A], docs: Documentation = None): Response[A] = {
    val _entity = entity
    new Response[A] {
      val entity: _entity.type = _entity
      def decodeResponse(response: sttp.Response[entity.ReceivedBody]) = {
        if (response.code == statusCode) entity.decodeEntity(response)
        else backend.responseMonad.error(new Throwable(s"Unexpected status code: ${response.code}"))
      }
    }
  }

  def wheneverFound[A](inner: Response[A], notFoundDocs: Documentation): Response[Option[A]] = new Response[Option[A]] {
    val entity = mapResponseEntity[A, Option[A]](inner.entity)(Some(_))
    def decodeResponse(response: sttp.Response[entity.ReceivedBody]): R[Option[A]] = {
      if (response.code == NotFound) backend.responseMonad.unit(None)
      else entity.decodeEntity(response)
    }
  }

  /**
    * A function that, given an `A`, eventually attempts to decode the `B` response.
    */
  //#endpoint-type
  type Endpoint[A, B] = A => R[B]
  //#endpoint-type

  def endpoint[A, B](request: Request[A], response: Response[B], summary: Documentation, description: Documentation, tags: List[String]): Endpoint[A, B] =
    a => {
      val req: sttp.Request[response.entity.ReceivedBody, Nothing] = request(a).response(response.entity.responseAs)
      val result = backend.send(req)
      backend.responseMonad.flatMap(result)(response.decodeResponse)
    }

}
