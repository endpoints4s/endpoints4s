package endpoints
package openapi

import endpoints.algebra.documented
import endpoints.documented.algebra
import endpoints.documented.openapi.Info
import org.scalatest.{Matchers, OptionValues, WordSpec}

class DocumentedEndpointsTest extends WordSpec with Matchers with OptionValues {

  "Path parameters" should {
    "Appear as patterns between braces in the documentation" in {
      DocumentedEndpointsFixtures.baz.path shouldBe "/baz/{quux}"
    }
  }

  "Endpoints sharing the same path" should {
    "be grouped together" in {

      val fooItem =
        DocumentedEndpointsFixtures.documentation.paths.get("/foo").value

      fooItem.operations.size shouldBe 2
      fooItem.operations.get("get") shouldBe defined
      fooItem.operations.get("post") shouldBe defined

    }
  }

}

trait DocumentedEndpointsFixtures extends algebra.Endpoints {

  val foo = endpoint(get(path / "foo"), emptyResponse("Foo response"))

  val bar = endpoint(post(path / "foo", emptyRequest), emptyResponse("Bar response"))

  val baz = endpoint(get(path / "baz" / segment[Int]("quux")), emptyResponse("Baz response"))

}

object DocumentedEndpointsFixtures
  extends DocumentedEndpointsFixtures
    with DocumentedEndpoints {

  val documentation = openApi(Info("Test API", "1.0.0"))(foo, bar, baz)

}