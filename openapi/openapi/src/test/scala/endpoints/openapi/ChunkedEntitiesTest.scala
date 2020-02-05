package endpoints.openapi

import endpoints.openapi.model.Schema.{Array, Primitive, Reference}
import endpoints.openapi.model._
import org.scalatest.wordspec.AnyWordSpec

class ChunkedEntitiesTest extends AnyWordSpec {

  "Chunked Endpoint" in {
    val expected =
      PathItem(Map("get" -> Operation(
        None,
        None,
        List(Parameter("file", In.Path, required = true, None, Schema.simpleString)),
        None,
        Map(
          "200" -> Response("", Map("application/octet-stream" -> MediaType(None))),
          "400" -> Response("Client error", Map("application/json" -> MediaType(Some(Reference("endpoints.Errors", Some(Array(Left(Primitive("string", None, None, None)), None, None)), None, None))))),
          "500" -> Response("Server error", Map("application/json" -> MediaType(Some(Reference("endpoints.Errors", Some(Array(Left(Primitive("string", None, None, None)), None, None)), None, None)))))
        ),
        Nil,
        Nil,
        Map.empty,
        false
      )))
    assert(Fixtures.documentation.paths("/assets2/{file}") == expected)
  }

}
