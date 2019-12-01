package endpoints.play.server

import endpoints.algebra.Codec
import endpoints.algebra

trait JsonSchemaEntities
  extends algebra.JsonSchemaEntities
    with JsonEntitiesFromCodec
    with endpoints.ujson.JsonSchemas {

  def stringCodec[A](implicit codec: JsonCodec[A]): Codec[String, A] = codec.stringCodec

}
