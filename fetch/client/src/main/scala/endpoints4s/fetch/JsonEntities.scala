package endpoints4s.fetch

import endpoints4s.Codec
import endpoints4s.algebra
import org.scalajs.dom.experimental.{RequestInit => FetchRequestInit}

import scala.concurrent.Future

trait JsonEntitiesFromCodecs extends EndpointsWithCustomErrors with algebra.JsonEntitiesFromCodecs {

  def jsonRequest[A](implicit codec: JsonCodec[A]) =
    (a: A, requestInit: FetchRequestInit) => {
      requestInit.setRequestHeader("Content-Type", "application/json")
      requestInit.body = stringCodec(codec).encode(a)
      Future.successful(())
    }

  def jsonResponse[A](implicit codec: JsonCodec[A]) =
    stringCodecResponse(stringCodec(codec))

}

trait JsonEntitiesFromSchemas
    extends algebra.JsonEntitiesFromSchemas
    with JsonEntitiesFromCodecs
    with endpoints4s.ujson.JsonSchemas {

  def stringCodec[A](implicit codec: JsonCodec[A]): Codec[String, A] =
    codec.stringCodec

}
