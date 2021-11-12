package endpoints4s.akkahttp.server

import endpoints4s.algebra
import org.scalatest.wordspec.AnyWordSpec

class RequestUriTest extends AnyWordSpec {

  trait Fixtures extends algebra.Endpoints {
    val p1 = path / "a" / "b/c" / segment[String]()
    val p2 = path / segment[String]() / segment[String]() / remainingSegments()
    val p3 =
      path / segment[String]() /? (qs[String]("q") & qs[Option[String]]("r") & optQsWithDefault[
        Int
      ]("s", 1) & qs[List[String]]("t"))
    val p4 = path / "a" /? qs[Option[Int]]("x")
    val e = endpoint(get(p4), ok(textResponse))
  }

  object Fixtures extends Fixtures with Endpoints

  "akka-http-server request interpreter" should {
    "properly encode URIs" in {
      import Fixtures.{p1, p2, p3, p4, e}
      assert(p1.uri("d/e").toString == "/a/b%2Fc/d%2Fe")
      assert(p2.uri(("a b", "c/d", "e/f")).toString == "/a%20b/c%2Fd/e/f")
      assert(p2.uri(("a b", "c/d", "")).toString == "/a%20b/c%2Fd/")
      assert(
        p3.uri(("a b", "c d", Some("e f"), 1, List("g h", "i j")))
          .toString == "/a%20b?q=c+d&r=e+f&s=1&t=g+h&t=i+j"
      )
      assert(p4.uri(None).toString == "/a")
      assert(e.uri(Some(42)).toString == "/a?x=42")
    }
  }

}
