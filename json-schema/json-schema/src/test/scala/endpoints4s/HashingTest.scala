package endpoints4s

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

object HashingTest extends Properties("Hashing") {

  property("hash") = forAll { (x: Int, y: Int, s: String, t: String) =>
    val oneValue =
      (x == y) == (Hashing.hash(x) == Hashing.hash(y))
    val twoValues =
      ((x, s) == ((y, t))) == (Hashing.hash(x, s) == Hashing.hash(y, t))
    oneValue && twoValues
  }

}
