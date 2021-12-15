package endpoints4s.xhr

import endpoints4s.algebra.MuxRequest
import endpoints4s.Decoder
import endpoints4s.Encoder
import endpoints4s.algebra

import scala.scalajs.js
import scala.scalajs.js.|

/** @group interpreters
  */
trait MuxEndpoints extends algebra.MuxEndpoints with EndpointsWithCustomErrors {

  protected final def muxPerformXhr[Req <: MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport],
      req: Req
  )(implicit
      encoder: Encoder[Req, Transport],
      decoder: Decoder[Transport, Resp]
  ): (js.Promise[req.Response], js.Function1[Unit, Unit]) = {
    val (value, abort) = performXhr(request, response, encoder.encode(req))
    (
      value
        .`then`(
          (b: Transport) => {
            decoder
              .decode(b)
              .asInstanceOf[Either[Throwable, req.Response]]
              .fold(
                th => js.Promise.reject(th),
                r => js.Promise.resolve[req.Response](r)
              ): req.Response | js.Thenable[req.Response]
          },
          js.undefined
        ),
      abort
    )
  }

}
