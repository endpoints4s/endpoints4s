package endpoints4s.fetch

import scala.scalajs.js

@js.native
sealed trait ChunksRequestDuplex extends js.Any

object ChunksRequestDuplex {
  val half: ChunksRequestDuplex = "half".asInstanceOf[ChunksRequestDuplex]
}
