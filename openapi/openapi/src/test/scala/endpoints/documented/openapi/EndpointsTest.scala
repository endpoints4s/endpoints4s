package endpoints
package documented
package openapi

import endpoints.documented.openapi.model.Info
import org.scalatest.{Matchers, OptionValues, WordSpec}

class EndpointsTest extends WordSpec with Matchers with OptionValues {

  "Path parameters" should {
    "Appear as patterns between braces in the documentation" in {
      Fixtures.baz.path shouldBe "/baz/{quux}"
    }
  }

  "Endpoints sharing the same path" should {
    "be grouped together" in {

      val fooItem =
        Fixtures.documentation.paths.get("/foo").value

      fooItem.operations.size shouldBe 2
      fooItem.operations.get("get") shouldBe defined
      fooItem.operations.get("post") shouldBe defined

    }
  }

}

trait Fixtures extends algebra.Endpoints {

  val foo = endpoint(get(path / "foo"), emptyResponse("Foo response"))

  val bar = endpoint(post(path / "foo", emptyRequest), emptyResponse("Bar response"))

  val baz = endpoint(get(path / "baz" / segment[Int]("quux")), emptyResponse("Baz response"))

}

object Fixtures
  extends Fixtures
    with Endpoints {

  val documentation = openApi(Info("Test API", "1.0.0"))(foo, bar, baz)

}