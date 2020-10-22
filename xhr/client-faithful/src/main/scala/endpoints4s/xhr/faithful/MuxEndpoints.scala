package endpoints4s.xhr.faithful

import endpoints4s.algebra.MuxRequest
import endpoints4s.{Decoder, Encoder, xhr}
import faithful.{Future, Promise}

/** @group interpreters
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
