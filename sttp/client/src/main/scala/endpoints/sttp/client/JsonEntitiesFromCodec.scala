package endpoints.sttp.client

import endpoints.algebra.Codec
import com.softwaremill.sttp

import scala.language.higherKinds

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON request
  * @group interpreters
  */
trait JsonEntitiesFromCodec[R[_]] extends endpoints.algebra.JsonEntitiesFromCodec { self: Endpoints[R] =>

  def jsonRequest[A](implicit codec: Codec[String, A]): RequestEntity[A] = { (a, req) =>
    req.body(codec.encode(a)).contentType("application/json")
  }

  def jsonResponse[A](implicit codec: Codec[String, A]): ResponseEntity[A] = new ResponseEntity[A] {
    def decodeEntity(response: sttp.Response[String]): R[A] = {
      response.body
        .left.map(new Throwable(_))
        .right.flatMap { entity =>
        codec.decode(entity).fold(Right(_), errors => Left(new Exception(errors.mkString(". "))))
      } match {
        case Right(a)        => backend.responseMonad.unit(a)
        case Left(exception) => backend.responseMonad.error(exception)
      }
    }
  }

}
