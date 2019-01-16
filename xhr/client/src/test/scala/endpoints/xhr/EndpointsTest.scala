package endpoints.xhr

import org.scalatest.FreeSpec

// Separation of the Algebra here is important, due to implementation details
// of the underlying representation of header in the xhr algebra.
trait FixturesAlgebra extends endpoints.algebra.Endpoints {
  val foo = endpoint(get(path / "foo" / segment[String]()), emptyResponse())
  val bar = endpoint(post(path / "bar" /? qs[Int]("quux"), emptyRequest), emptyResponse())
  // Currently, the fact that this line compiles is a test, as there's no way
  // to inspect the result of constructing headers at the moment.
  val baz = endpoint(post(path / "baz", emptyRequest, header("quuz") ++ header("corge") ++ optHeader("grault")), emptyResponse())
}

object Fixtures extends FixturesAlgebra with thenable.Endpoints

// TODO try to use traits defined in algebra tests.
// It cannot be simply reused because dependency on wiremock which is not available for js
class EndpointsTest extends FreeSpec {

  "href" in {
    assert("/foo/hello%20world" == Fixtures.foo.href("hello world"))
    assert("/bar?quux=42" == Fixtures.bar.href(42))
  }

}
