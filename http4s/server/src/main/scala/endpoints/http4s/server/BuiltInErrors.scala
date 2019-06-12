package endpoints.http4s.server

import java.nio.charset.StandardCharsets

import endpoints.{Invalid, algebra}
import fs2.Chunk
import org.http4s.EntityEncoder
import org.http4s.MediaType
import org.http4s.headers.`Content-Type`

/**
  * @group interpreters
  */
trait BuiltInErrors extends algebra.BuiltInErrors { this: EndpointsWithCustomErrors =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] = {
    val hdr = `Content-Type`(MediaType.application.json)
    EntityEncoder.simple(hdr) { invalid =>
      val s = endpoints.ujson.codecs.invalidCodec.encode(invalid)
      Chunk.bytes(s.getBytes(StandardCharsets.UTF_8))
    }
  }

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    clientErrorsResponseEntity.contramap(th => Invalid(th.getMessage))
}
