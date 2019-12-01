package endpoints.akkahttp.client

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import endpoints.algebra

trait JsonSchemaEntities
  extends algebra.JsonSchemaEntities
    with EndpointsWithCustomErrors
    with endpoints.ujson.JsonSchemas {

  def jsonRequest[A](implicit codec: JsonRequest[A]): RequestEntity[A] = { (a, req) =>
    req.copy(
      entity = HttpEntity(
        ContentTypes.`application/json`,
        codec.stringCodec.encode(a)
      )
    )
  }

  def jsonResponse[A](implicit codec: JsonResponse[A]): ResponseEntity[A] =
    stringCodecResponse(codec.stringCodec)
}
