package endpoints4s.xhr.thenable

import endpoints4s.algebra.MuxRequest
import endpoints4s.{Decoder, Encoder, xhr}

import scala.scalajs.js

/** @group interpreters
  */
trait MuxEndpoints extends xhr.MuxEndpoints with EndpointsWithCustomErrors {

  class MuxEndpoint[Req <: MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ) {
    def apply(
        req: Req
    )(implicit
        encoder: Encoder[Req, Transport],
        decoder: Decoder[Transport, Resp]
    ): Result[req.Response] = {
      var jsAbort: js.Function0[Unit] = null
      val promise = new js.Promise[req.Response]((resolve, error) => {
        jsAbort = muxPerformXhr(request, response, req)(
          _.fold(exn => { error(exn.getMessage); () }, resp => { resolve(resp); () }),
          throwable => { error(throwable.toString); () }
        )
      })
      new Result(promise) {
        def abort(): Unit = jsAbort()
      }
    }
  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}
