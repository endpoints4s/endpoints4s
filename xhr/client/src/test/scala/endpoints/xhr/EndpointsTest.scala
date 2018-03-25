package endpoints.xhr

import org.scalatest.FreeSpec

object Fixtures extends thenable.Endpoints {
  val foo = endpoint(get(path / "foo" / segment[String]), emptyResponse)
  val bar = endpoint(post(path / "bar" /? qs[Int]("quux"), emptyRequest), emptyResponse)
}

class EndpointsTest extends FreeSpec {

  "href" in {
    assert("/foo/hello%20world" == Fixtures.foo.href("hello world"))
    assert("/bar?quux=42" == Fixtures.bar.href(42))
  }

}
