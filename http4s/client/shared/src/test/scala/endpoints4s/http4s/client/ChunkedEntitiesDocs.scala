package endpoints4s.http4s.client

import endpoints4s.algebra
import cats.effect.IO
import cats.effect.Resource

trait ChunkedEntitiesDefinitions extends algebra.ChunkedEntities {
  //#streamed-endpoint
  val logo: Endpoint[Unit, Chunks[Array[Byte]]] =
    endpoint(get(path / "logo.png"), ok(bytesChunksResponse))
  //#streamed-endpoint
}

trait ChunkedEntitiesDocs extends ChunkedEntitiesDefinitions with ChunkedEntities {
  this: Endpoints[IO] =>
  //#invocation
  val bytesSource: Resource[Effect, fs2.Stream[Effect, Array[Byte]]] =
    logo.send(())

  bytesSource.use(stream =>
    stream.evalMap { bytes => IO(println(s"Received ${bytes.length} bytes")) }.compile.drain
  )
  //#invocation
}
