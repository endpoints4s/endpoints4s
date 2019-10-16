package endpoints.algebra

import endpoints.{Invalid, Valid}
import org.scalatest.WordSpec

class InvalidCodecTest extends WordSpec {

  "InvalidCodec" should {
    "Properly handle double quotes in strings" in {
      val decoded = Invalid(Seq("foo", "bar\",", "", "ba\nz"))
      val encoded = """["foo","bar\",","","ba\nz"]"""
      assert(InvalidCodec.invalidCodec.encode(decoded) == encoded)
      assert(InvalidCodec.invalidCodec.decode(encoded) == Valid(decoded))
    }
  }

}
