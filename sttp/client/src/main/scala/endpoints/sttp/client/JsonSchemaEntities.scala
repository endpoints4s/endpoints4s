package endpoints.sttp.client

import com.softwaremill.sttp
import endpoints.algebra.Documentation
import endpoints.{algebra, circe}
import io.circe.parser

import language.higherKinds

trait JsonSchemaEntities[R[_]] extends Endpoints[R] with algebra.JsonSchemaEntities with circe.JsonSchemas {

  def jsonRequest[A : JsonRequest](docs: Documentation = None): RequestEntity[A] = { (a, request) =>
    request
      .body(JsonSchema.toCirceEncoder[A].apply(a).noSpaces)
      .contentType("application/json")
  }

  def jsonResponse[A : JsonResponse](docs: Documentation = None): Response[A] = new SttpResponse[A] {
    override type ReceivedBody = Either[Exception, A]
    override def responseAs = sttp.asString.map { str =>
      parser.parse(str).flatMap(JsonSchema.toCirceDecoder[A].decodeJson)
    }
    override def validateResponse(response: sttp.Response[ReceivedBody]): R[A] = {
      response.unsafeBody match {
        case Right(a) => backend.responseMonad.unit(a)
        case Left(exception) => backend.responseMonad.error(exception)
      }
    }
  }
}
