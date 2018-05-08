package endpoints.xhr

import org.scalatest.FreeSpec

object Fixtures extends thenable.Endpoints {
  val foo = endpoint(get(path / "foo" / segment[String]()), emptyResponse())
  val bar = endpoint(post(path / "bar" /? qs[Int]("quux"), emptyRequest), emptyResponse())
}

// TODO try to use traits defined in algebra tests.
// It cannot be simply reused because dependency on wiremock which is not available for js
class EndpointsTest extends FreeSpec {

  "href" in {
    assert("/foo/hello%20world" == Fixtures.foo.href("hello world"))
    assert("/bar?quux=42" == Fixtures.bar.href(42))
  }

}
