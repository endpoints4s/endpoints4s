package endpoints4s.play.client

import play.api.http.ContentTypes
import play.api.libs.ws.{BodyWritable, InMemoryBody}

/** Interpreter for [[endpoints4s.algebra.JsonEntitiesFromCodecs]] that encodes JSON requests
  * and decodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodecs
    extends EndpointsWithCustomErrors
    with endpoints4s.algebra.JsonEntitiesFromCodecs {

  def jsonRequest[A](implicit codec: JsonCodec[A]): RequestEntity[A] = { (a, wsRequest) =>
    val playCodec: play.api.mvc.Codec = implicitly[play.api.mvc.Codec]
    val writable = BodyWritable(
      (s: String) => InMemoryBody(playCodec.encode(s)),
      ContentTypes.JSON
    )
    wsRequest.withBody(stringCodec(codec).encode(a))(writable)
  }

  def jsonResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[A] =
    wsResp =>
      stringCodec(codec)
        .decode(wsResp.body)
        .fold(Right(_), errors => Left(new Exception(errors.mkString(". "))))

}
