package endpoints.xhr

import endpoints.algebra
import endpoints.algebra.Codec

trait JsonSchemaEntities
  extends algebra.JsonSchemaEntities
    with JsonEntitiesFromCodec
    with endpoints.ujson.JsonSchemas {

  def stringCodec[A](implicit codec: JsonCodec[A]): Codec[String, A] = codec.stringCodec

}
