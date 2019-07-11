package endpoints.akkahttp.server
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import endpoints.algebra.Documentation

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that decodes JSON requests and
  * encodes JSON responses using Akka HTTP.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodec extends Endpoints with endpoints.algebra.JsonEntitiesFromCodec {

  def jsonRequest[A](docs: Documentation)(implicit codec: JsonCodec[A]): RequestEntity[A] = {
    implicit val fromEntityUnmarshaller: FromEntityUnmarshaller[A] =
      Unmarshaller.stringUnmarshaller
        .forContentTypes(MediaTypes.`application/json`)
        .map(data => jsonCodecToCodec(codec).decode(data).fold(throw _, identity))
    Directives.entity[A](implicitly)
  }

  def jsonResponse[A](docs: Documentation)(implicit codec: JsonCodec[A]): Response[A] = { a =>
    implicit val toEntityMarshaller: ToEntityMarshaller[A] =
      Marshaller.withFixedContentType(MediaTypes.`application/json`) { value =>
        HttpEntity(MediaTypes.`application/json`, jsonCodecToCodec(codec).encode(value))
      }
    Directives.complete(a)
  }

}
