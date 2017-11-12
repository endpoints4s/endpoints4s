package endpoints.play.server.circe

import endpoints.algebra
import endpoints.play.server.Endpoints
import io.circe.{Decoder => CirceDecoder, Encoder => CirceEncoder, jawn}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Results}
import endpoints.play.server.Util.circeJsonWriteable

/**
  * Interpreter for [[algebra.JsonEntities]] that uses circe’s [[io.circe.Decoder]] to decode
  * JSON entities in HTTP requests, and circe’s [[io.circe.Encoder]] to build JSON entities
  * in HTTP responses.
  */
trait JsonEntities extends Endpoints with algebra.JsonEntities {

  /** Decode requests using circe’s [[io.circe.Decoder]] */
  type JsonRequest[A] = CirceDecoder[A]

  /** Encode responses using circe’s [[io.circe.Encoder]] */
  type JsonResponse[A] = CirceEncoder[A]

  def jsonRequest[A : CirceDecoder]: RequestEntity[A] =
    BodyParsers.parse.raw.validate { buffer =>
      jawn.parseFile(buffer.asFile)
        .right.flatMap(CirceDecoder[A].decodeJson)
        .left.map(error => Results.BadRequest)
    }

  def jsonResponse[A : CirceEncoder]: Response[A] = a => Results.Ok(CirceEncoder[A].apply(a))

}
