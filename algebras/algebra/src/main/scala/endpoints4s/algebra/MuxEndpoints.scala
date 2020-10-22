package endpoints4s.algebra

/** Algebra interface for describing endpoints such that one endpoint can
  * handle several types of requests and responses.
  *
  * @group algebras
  */
trait MuxEndpoints extends EndpointsWithCustomErrors {

  /** Information carried by a multiplexed HTTP endpoint.
    * @group types
    */
  type MuxEndpoint[Req <: MuxRequest, Resp, Transport]

  /** Multiplexed HTTP endpoint.
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
    * @group operations
    */
  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport]

}

/** Multiplexed request type
  */
trait MuxRequest {

  /** Type of the response for `this` specific request */
  type Response
}
