package endpoints4s
package akkahttp.client

import endpoints4s.algebra.MuxRequest

import scala.concurrent.Future

/** @group interpreters
  */
trait MuxEndpoints extends algebra.MuxEndpoints {
  self: EndpointsWithCustomErrors =>

  class MuxEndpoint[Req <: algebra.MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ) {
    def apply(
        req: Req
    )(implicit
        encoder: Encoder[Req, Transport],
        decoder: Decoder[Transport, Resp]
    ): Future[req.Response] =
      for {
        req2 <- request(encoder.encode(req))
        resp <- settings.requestExecutor(req2)
        entity <- futureFromEither(
          decodeResponse(response, resp).toRight({
            resp.discardEntityBytes()
            new Throwable(s"Unexpected response status or headers")
          })
        )
        t <- entity(resp.entity)
        tt <- futureFromEither(t)
        ttt <- futureFromEither(
          decoder
            .decode(tt)
            .fold(
              resp => Right(resp.asInstanceOf[req.Response]),
              errors => Left(new Exception(errors.mkString(". ")))
            )
        )
      } yield ttt
  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}
