package endpoints.xhr.thenable

import endpoints.algebra.{Decoder, Encoder, MuxRequest}
import endpoints.xhr

import scala.scalajs.js

/**
  * @group interpreters
  */
trait MuxEndpoints extends xhr.MuxEndpoints with Endpoints {

  class MuxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ) {
    def apply(
      req: Req
    )(implicit
      encoder: Encoder[Req, Transport],
      decoder: Decoder[Transport, Resp]
    ): js.Thenable[req.Response] = {
      new js.Promise[req.Response]((resolve, error) => {
        muxPerformXhr(request, response, req)(
          _.fold(exn => error(exn.getMessage), resp => resolve(resp)),
          xhr => error(xhr.responseText)
        )
      })
    }
  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}
