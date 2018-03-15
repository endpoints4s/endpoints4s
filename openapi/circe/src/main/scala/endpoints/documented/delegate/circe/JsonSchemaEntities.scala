package endpoints.documented.delegate.circe

import endpoints.algebra.circe.CirceCodec
import io.circe.{Decoder, Encoder}

/**
  * Interpreter for [[endpoints.documented.circe.JsonSchemas]] that
  * delegates to an [[endpoints.algebra.circe.JsonEntitiesFromCodec]] interpreter.
  */
trait JsonSchemaEntities
  extends endpoints.documented.delegate.JsonSchemaEntities
    with endpoints.documented.circe.JsonSchemas {

  val delegate: endpoints.algebra.circe.JsonEntitiesFromCodec

  implicit def jsonSchemaToDelegateJsonCodec[A](implicit jsonSchema: JsonSchema[A]): delegate.JsonCodec[A] =
    new CirceCodec[A] {
      def decoder: Decoder[A] = jsonSchema.decoder
      def encoder: Encoder[A] = jsonSchema.encoder
    }

}
