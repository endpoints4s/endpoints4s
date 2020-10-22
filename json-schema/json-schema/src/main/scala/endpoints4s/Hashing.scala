package endpoints4s

import scala.util.hashing.MurmurHash3

private[endpoints4s] object Hashing {

  /** @return a hash for the given `values`, computed using the MurmurHash3 algorithm
    *
    * Use this method to implement the `hashCode` operation of data classes:
    *
    * {{{
    *   final class Foo private (val x: Int, val s: String) {
    *     override def hashCode() = Hashing.hash(x, s)
    *     // ... also override equals!
    *   }
    * }}}
    */
  def hash(values: Any*): Int = {
    // The implementation has been copied and adapted from `MurmurHash3.productHash`
    require(values.nonEmpty)
    var h = MurmurHash3.productSeed
    for (value <- values) {
      h = MurmurHash3.mix(h, value.##)
    }
    MurmurHash3.finalizeHash(h, values.size)
  }

}
