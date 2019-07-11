package endpoints.sttp.client

import com.softwaremill.sttp
import endpoints.algebra.Documentation

import scala.language.higherKinds

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON request
  * @group interpreters
  */
trait JsonEntitiesFromCodec[R[_]] extends endpoints.algebra.JsonEntitiesFromCodec { self: Endpoints[R] =>

  def jsonRequest[A](docs: Documentation)(implicit codec: JsonCodec[A]): RequestEntity[A] = { (a, req) =>
    req.body(jsonCodecToCodec(codec).encode(a)).contentType("application/json")
  }

  def jsonResponse[A](docs: Documentation)(implicit codec: JsonCodec[A]): Response[A] = new SttpResponse[A] {
    override type ReceivedBody = Either[Exception, A]
    override def responseAs = sttp.asString.map(str => jsonCodecToCodec(codec).decode(str))
    override def validateResponse(response: sttp.Response[ReceivedBody]): R[A] = {
      response.unsafeBody match {
        case Right(a) => backend.responseMonad.unit(a)
        case Left(exception) => backend.responseMonad.error(exception)
      }
    }
  }

}
