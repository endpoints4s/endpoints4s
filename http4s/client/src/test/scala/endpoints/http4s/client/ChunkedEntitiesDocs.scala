package endpoints.http4s.client

import endpoints.algebra
import cats.effect.IO

trait ChunkedEntitiesDocs
    extends algebra.ChunkedEntitiesDocs
    with ChunkedEntities { this: Endpoints[IO] =>

  //#invocation
  val bytesSource: fs2.Stream[Effect, Array[Byte]] =
    fs2.Stream.force(logo(()))

  bytesSource
    .evalMap { bytes => IO(println(s"Received ${bytes.length} bytes")) }
    .compile
    .drain
  //#invocation

}
