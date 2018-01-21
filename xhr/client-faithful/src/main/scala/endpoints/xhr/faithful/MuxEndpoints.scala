package endpoints.xhr.faithful

import endpoints.algebra.{Decoder, Encoder, MuxRequest}
import endpoints.xhr
import faithful.{Future, Promise}

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
    ): Future[req.Response] = {
      val promise = new Promise[req.Response]()
      muxPerformXhr(request, response, req)(
        _.fold(promise.failure, promise.success),
        xhr => promise.failure(new Exception(xhr.responseText))
      )
      promise.future
    }
  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}
