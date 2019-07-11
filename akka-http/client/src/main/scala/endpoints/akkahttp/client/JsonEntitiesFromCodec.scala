package endpoints.akkahttp.client

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import endpoints.algebra.Documentation

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON request
  * and decodes JSON responses using Akka HTTP.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodec extends endpoints.algebra.JsonEntitiesFromCodec { this: Endpoints =>

  def jsonRequest[A](docs: Documentation)(implicit codec: JsonCodec[A]): RequestEntity[A] = { (a, req) =>
    req.copy(entity = HttpEntity(ContentTypes.`application/json`, jsonCodecToCodec(codec).encode(a)))
  }

  def jsonResponse[A](docs: Documentation)(implicit codec: JsonCodec[A]): Response[A] = { response =>
    for {
      strictEntity <- response.entity.toStrict(settings.toStrictTimeout)
    } yield jsonCodecToCodec(codec).decode(settings.stringContentExtractor(strictEntity))
  }

}
