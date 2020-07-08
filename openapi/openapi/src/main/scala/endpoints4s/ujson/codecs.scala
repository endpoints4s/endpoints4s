package endpoints4s.ujson

import endpoints4s.{Codec, Invalid, Validated, openapi}
import ujson.StringRenderer

/** Utility objects internally used by ''endpoints''. */
object codecs {

  object schemas extends openapi.JsonSchemas {

    /** The default schema for representing `Invalid` values is a JSON array containing error strings */
    val invalid: JsonSchema[Invalid] =
      arrayJsonSchema[Seq, String]
        .xmap(errors => Invalid(errors))(_.errors)
  }

  val stringJson: Codec[String, ujson.Value] =
    Codec.fromEncoderAndDecoder((json: ujson.Value) => json.transform(StringRenderer()).toString)(
      from =>
        Validated.fromEither(
          util.control.Exception.nonFatalCatch
            .either(ujson.read(from))
            .left
            .map(_ => "Invalid JSON document" :: Nil)
        )
    )

  val invalidCodec: Codec[String, Invalid] =
    Codec.sequentially(stringJson)(schemas.invalid.ujsonSchema.codec)

}
