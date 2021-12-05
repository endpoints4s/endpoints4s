package endpoints4s.algebra

trait CounterCodec { this: JsonCodecs =>
  case class Counter(value: Int)
  implicit def counterCodec: JsonCodec[Counter]
}
