package endpoints.sttp.client

import java.net.URI

import endpoints.algebra
import endpoints.Tupler
import com.softwaremill.sttp

import scala.language.higherKinds

/**
  * An interpreter for [[endpoints.algebra.Endpoints]] that builds a client issuing requests using
  * a sttp’s [[com.softwaremill.sttp.SttpBackend]].
  *
  * Doest not support streaming responses for now
  *
  * @param host     Base of the URL of the service that implements the endpoints (e.g. "http://foo.com")
  * @param backend  The underlying backend to use
  * @tparam R       The monad wrapping the response. It is defined by the backend
  */
class Endpoints[R[_]](host: String, val backend: sttp.SttpBackend[R, Nothing]) extends algebra.Endpoints with Urls with Methods {

  /**
    * A function that, given an `A` and a request model, returns an updated request
    * containing additional headers
    */
  type RequestHeaders[A] = (A, sttp.Request[_, Nothing]) => sttp.Request[_, Nothing]

  /** Does not modify the request */
  lazy val emptyHeaders: RequestHeaders[Unit] = (_, request) => request

  /**
    * A function that takes an `A` information and returns a `sttp.Request`
    */
  type Request[A] = A => sttp.Request[_, Nothing]

  /**
    * A function that, given an `A` information and a `sttp.Request`, eventually returns a `sttp.Request`
    */
  type RequestEntity[A] = (A, sttp.Request[_, Nothing]) => sttp.Request[_, Nothing]

  lazy val emptyRequest: RequestEntity[Unit] = { case (_, req) => req }

  def request[A, B, C, AB](
    method: Method, url: Url[A],
    entity: RequestEntity[B], headers: RequestHeaders[C]
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
    (abc: tuplerABC.Out) => {
      val (ab, c) = tuplerABC.unapply(abc)
      val (a, b) = tuplerAB.unapply(ab)

      val uri: sttp.Id[sttp.Uri] = sttp.Uri(new URI(s"${host}${url.encode(a)}"))
      val reqId: sttp.Request[_, Nothing] = method(sttp.sttp.get(uri = uri))
      entity(b, headers(c, reqId))
    }

  /**
    * Trait that indicates how a response should be interpreted
    */
  trait SttpResponse[A] {
    /**
      * The type of the received body from the server
      */
    type RB

    /**
      * To read the response body
      */
    def responseAs: sttp.ResponseAs[RB, Nothing]

    /**
      * Function to validate the response (headers, code). This can also modify the type of the received body
      */
    def validateResponse(response: sttp.Response[RB]): Either[String, A]
  }

  type Response[A] = SttpResponse[A]

  /** Successfully decodes no information from a response */
  val emptyResponse: Response[Unit] = new SttpResponse[Unit] {
    type RB = Unit
    override def responseAs = sttp.ignore
    override def validateResponse(response: sttp.Response[Unit]) = {
      if (response.isSuccess) response.body
      else Left(s"Unexpected status code: ${response.code}")
    }
  }

  /** Successfully decodes string information from a response */
  val textResponse: Response[String] = new SttpResponse[String] {
    type RB = String
    override def responseAs = sttp.asString
    override def validateResponse(response: sttp.Response[String]) = {
      if (response.isSuccess) response.body
      else Left(s"Unexpected status code: ${response.code}")
    }
  }

  /**
    * A function that, given an `A`, eventually attempts to decode the `B` response.
    */
  type Endpoint[A, B] = A => R[Either[String, B]]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    a => {
      val req: sttp.Request[response.RB, Nothing] = request(a).response(response.responseAs)

      val result = backend.send(req)
      backend.responseMonad.map(result)(response.validateResponse)
    }

}
