package endpoints.sttp.client

import endpoints.algebra.Codec
import com.softwaremill.sttp

import scala.language.higherKinds

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON request
  */
trait JsonEntitiesFromCodec[R[_]] extends endpoints.algebra.JsonEntitiesFromCodec { self: Endpoints[R] =>

  def jsonRequest[A](implicit codec: Codec[String, A]): RequestEntity[A] = { (a, req) =>
    req.body(codec.encode(a)).contentType("application/json")
  }

  def jsonResponse[A](implicit codec: Codec[String, A]): Response[A] = new SttpResponse[A] {
    override type RB = Either[Exception, A]
    override def responseAs = sttp.asString.map(str => codec.decode(str))
    override def validateResponse(response: sttp.Response[RB]): Either[String, A] = {
      response.body.right.flatMap {
        case Right(a) => Right(a)
        case Left(exception) => Left(s"Could not decode entity: $exception")
      }
    }
  }

}
