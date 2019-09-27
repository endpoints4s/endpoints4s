package endpoints.akkahttp.client

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import endpoints.algebra.Codec

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON request
  * and decodes JSON responses using Akka HTTP.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodec extends endpoints.algebra.JsonEntitiesFromCodec { this: Endpoints =>

  def jsonRequest[A](implicit codec: Codec[String, A]): RequestEntity[A] = { (a, req) =>
    req.copy(entity = HttpEntity(ContentTypes.`application/json`, codec.encode(a)))
  }

  def jsonResponse[A](implicit codec: Codec[String, A]): ResponseEntity[A] = { entity =>
    for {
      strictEntity <- entity.toStrict(settings.toStrictTimeout)
    } yield {
      codec.decode(settings.stringContentExtractor(strictEntity))
        .fold(Right(_), errors => Left(new Exception(errors.mkString(". "))))
    }
  }

}
