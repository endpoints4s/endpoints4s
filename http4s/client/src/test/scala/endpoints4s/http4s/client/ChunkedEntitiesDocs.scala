package endpoints4s.http4s.client

import endpoints4s.algebra
import cats.effect.IO
import cats.effect.Resource

trait ChunkedEntitiesDocs extends algebra.ChunkedEntitiesDocs with ChunkedEntities {
  this: Endpoints[IO] =>

  //#invocation
  val bytesSource: Resource[Effect, fs2.Stream[Effect, Array[Byte]]] =
    logo.send(())

  bytesSource.use(stream =>
    stream.evalMap { bytes => IO(println(s"Received ${bytes.length} bytes")) }.compile.drain
  )
  //#invocation

}
