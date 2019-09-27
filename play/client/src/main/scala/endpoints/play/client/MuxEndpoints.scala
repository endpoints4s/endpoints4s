package endpoints
package play.client

import endpoints.algebra.{Decoder, Encoder, MuxRequest}
import endpoints.play.client.Endpoints.futureFromEither

import scala.concurrent.Future

/**
  * @group interpreters
  */
trait MuxEndpoints extends algebra.Endpoints { self: Endpoints =>

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
      request(encoder.encode(req)).flatMap { wsResponse =>
        futureFromEither(
          response(wsResponse.status, wsResponse.headers)
            .toRight(new Throwable(s"Unexpected response status: ${wsResponse.status}")).right.flatMap { entity =>
              entity(wsResponse).right.flatMap { t =>
                decoder.decode(t)
                  .fold(resp => Right(resp.asInstanceOf[req.Response]), errors => Left(new Exception(errors.mkString(". "))))
              }
            }
        )
      }
  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}
