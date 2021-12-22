package endpoints4s.xhr

import endpoints4s.algebra.MuxRequest
import endpoints4s.Decoder
import endpoints4s.Encoder
import endpoints4s.algebra

import scala.scalajs.js

/** @group interpreters
  */
trait MuxEndpoints extends algebra.MuxEndpoints with EndpointsWithCustomErrors {

  protected final def muxPerformXhr[Req <: MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport],
      req: Req
  )(
      onload: Either[Throwable, req.Response] => Unit,
      onError: Throwable => Unit
  )(implicit
      encoder: Encoder[Req, Transport],
      decoder: Decoder[Transport, Resp]
  ): js.Function0[Unit] = {
    val abort = performXhr(request, response, encoder.encode(req))(
      errorOrResp =>
        onload(
          errorOrResp.flatMap(decoder.decode(_).asInstanceOf[Either[Throwable, req.Response]])
        ),
      onError
    )
    abort
  }

}
