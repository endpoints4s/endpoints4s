package endpoints.scalaj.client

import endpoints.algebra
import endpoints.algebra.CirceEntities.CirceCodec
import io.circe.parser

import scalaj.http.HttpRequest

trait CirceEntities extends algebra.CirceEntities { self: Endpoints =>

  /** Builds a request entity by using the supplied codec */
  def jsonRequest[A : CirceCodec]: RequestEntity[A] = (data: A, request: HttpRequest) => {
      request.header("Content-Type", "application/json")
      val jsonStr = CirceCodec[A].encoder.apply(data).noSpaces
      request.postData(jsonStr)
    }

  /** Decodes the response entity by using the supplied codec */
  def jsonResponse[A : CirceCodec]: Response[A] =
    resp => parser.parse(resp.body).right.flatMap(CirceCodec[A].decoder.decodeJson _)


}
