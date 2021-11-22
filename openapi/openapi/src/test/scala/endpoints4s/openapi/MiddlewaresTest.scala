package endpoints4s.openapi

import endpoints4s.algebra
import endpoints4s.openapi.model.Schema
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MiddlewaresTest extends AnyWordSpec with Matchers {

  object Documentation extends algebra.EndpointsTestApi with Endpoints

  "Documentation of mapped requests is enriched with additional information" in {
    Documentation.mappedEndpointRight.request.headers.value shouldBe List(
      Documentation.DocumentedHeader("If-None-Match", None, true, Schema.simpleString),
      Documentation.DocumentedHeader("If-Modified-Since", None, true, Schema.simpleString)
    )
    Documentation.mappedEndpointRight.request.url.queryParameters shouldBe List(
      Documentation.DocumentedParameter("x", true, None, Schema.simpleInteger),
      Documentation.DocumentedParameter("y", true, None, Schema.simpleInteger)
    )
  }

  "Documentation of mapped response is enriched with additional information" in {
    Documentation.mappedEndpointRight.response shouldBe List(
      Documentation.DocumentedResponse(304, "", Documentation.DocumentedHeaders(Nil), Map.empty),
      Documentation.DocumentedResponse(
        200,
        "",
        Documentation.DocumentedHeaders(
          List(
            Documentation.DocumentedHeader("ETag", None, true, Schema.simpleString),
            Documentation.DocumentedHeader("Last-Modified", None, true, Schema.simpleString)
          )
        ),
        Map.empty
      )
    )
  }

  "Documentation of mapped endpoints is transformed" in {
    Documentation.mappedEndpointRight.docs.summary shouldBe Some("Initial summary (mapped)")
  }

}
