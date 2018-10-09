package endpoints.sttp.client

import endpoints.algebra.{Codec, Documentation}
import com.softwaremill.sttp

import scala.language.higherKinds

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON request
  * @group interpreters
  */
trait JsonEntitiesFromCodec[R[_]] extends endpoints.algebra.JsonEntitiesFromCodec { self: Endpoints[R] =>

  def jsonRequest[A](docs: Documentation)(implicit codec: Codec[String, A]): RequestEntity[A] = { (a, req) =>
    req.body(codec.encode(a)).contentType("application/json")
  }

  def jsonResponse[A](docs: Documentation)(implicit codec: Codec[String, A]): Response[A] = new SttpResponse[A] {
    override type ReceivedBody = Either[Exception, A]
    override def responseAs = sttp.asString.map(str => codec.decode(str))
    override def validateResponse(response: sttp.Response[ReceivedBody]): R[A] = {
      response.unsafeBody match {
        case Right(a) => backend.responseMonad.unit(a)
        case Left(exception) => backend.responseMonad.error(exception)
      }
    }
  }

}
