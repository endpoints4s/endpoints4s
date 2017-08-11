package endpoints.akkahttp.client

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import endpoints.algebra
import endpoints.algebra.CirceEntities.CirceCodec
import io.circe.jawn

import scala.concurrent.Future

trait CirceEntities extends algebra.CirceEntities { this: Endpoints =>

  /** Builds a request entity by using the supplied codec */
  def jsonRequest[A : CirceCodec]: RequestEntity[A] = {
    case (a, req) => req.copy(entity = HttpEntity(implicitly[CirceCodec[A]].encoder(a).noSpaces))
  }

  /** Decodes a response entity by using the supplied codec */
  def jsonResponse[A : CirceCodec]: Response[A] =
    response =>
      if(response.status == StatusCodes.OK) {
        for {
          strictResponse <- response.entity.toStrict(settings.toStrictTimeout)
          json <- jawn.parse(settings.stringContentExtractor(strictResponse)) match {
            case Left(err) => Future.failed(new Throwable(err.message))
            case Right(v) => Future.successful(v)
          }
          res <- CirceCodec[A].decoder.decodeJson(json) match {
            case Left(err) => Future.successful(Left(new Throwable(err.message)))
            case Right(v) => Future.successful(Right(v))
          }
        } yield res
      } else {
        Future.failed(new Throwable(s"Unexpected status code: ${response.status.intValue()}"))
      }
}
