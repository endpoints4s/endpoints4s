package endpoints
package algebra

import scala.language.higherKinds

/**
  * Algebra interface for describing endpoints including documentation
  * (such as human readable descriptions of things).
  *
  * This interface is modeled after [[Endpoints]] but some methods
  * take additional parameters carrying the documentation part.
  */
trait DocumentedEndpoints
  extends DocumentedRequests
    with DocumentedResponses {

  /**
    * Information carried by an HTTP endpoint
    * @tparam A Information carried by the request
    * @tparam B Information carried by the response
    */
  type Endpoint[A, B]

  /**
    * HTTP endpoint.
    *
    * @param request Request
    * @param response Response
    */
  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B]

  /**
    * Information carried by a multiplexed HTTP endpoint.
    */
  type MuxEndpoint[Req <: MuxRequest, Resp, Transport]

  /**
    * Multiplexed HTTP endpoint.
    *
    * A multiplexing endpoint makes it possible to use several request
    * and response types in the same HTTP endpoint. In other words, it
    * allows to define several different actions through a singe HTTP
    * endpoint.
    *
    * @param request The request
    * @param response The response
    * @tparam Req The base type of possible requests
    * @tparam Resp The base type of possible responses
    * @tparam Transport The data type used to transport the requests and responses
    */
  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport]

}
