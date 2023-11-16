package endpoints4s.akkahttp.client

import endpoints4s.algebra

trait ChunkedEntitiesDefinitions extends algebra.ChunkedEntities {
  //#streamed-endpoint
  val logo: Endpoint[Unit, Chunks[Array[Byte]]] =
    endpoint(get(path / "logo.png"), ok(bytesChunksResponse))
  //#streamed-endpoint
}

trait ChunkedEntitiesDocs extends ChunkedEntitiesDefinitions with ChunkedEntities {
  this: Endpoints =>

  //#invocation
  import akka.stream.scaladsl.Source

  val bytesSource: Source[Array[Byte], _] =
    Source.futureSource(logo(()))

  bytesSource.runForeach { bytes => println(s"Received ${bytes.length} bytes") }
  //#invocation

}
