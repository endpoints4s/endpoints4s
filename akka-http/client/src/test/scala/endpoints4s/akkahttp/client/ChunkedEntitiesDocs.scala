package endpoints4s.akkahttp.client

import endpoints4s.algebra

trait ChunkedEntitiesDocs extends algebra.ChunkedEntitiesDocs with ChunkedEntities {
  this: Endpoints =>

  //#invocation
  import akka.stream.scaladsl.Source

  val bytesSource: Source[Array[Byte], _] =
    Source.futureSource(logo(()))

  bytesSource.runForeach { bytes => println(s"Received ${bytes.length} bytes") }
  //#invocation

}
