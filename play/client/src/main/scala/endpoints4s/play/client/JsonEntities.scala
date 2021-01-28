package endpoints4s.play.client

import play.api.http.ContentTypes
import play.api.libs.ws.{BodyWritable, InMemoryBody}
import endpoints4s.{Codec, algebra, ujson}

/** Interpreter for [[endpoints4s.algebra.JsonEntitiesFromCodecs]] that encodes JSON requests
  * and decodes JSON responses using Play WS.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodecs extends algebra.JsonEntitiesFromCodecs with EndpointsWithCustomErrors {

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

/** Interpreter for [[endpoints4s.algebra.JsonEntitiesFromSchemas]] that encodes JSON requests
  * and decodes JSON responses using Play WS.
  *
  * @group interpreters
  */
trait JsonEntitiesFromSchemas
    extends algebra.JsonEntitiesFromSchemas
    with JsonEntitiesFromCodecs
    with ujson.JsonSchemas {

  def stringCodec[A](implicit codec: JsonCodec[A]): Codec[String, A] =
    codec.stringCodec

}
