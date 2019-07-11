package endpoints.openapi

import org.scalatest.WordSpec
import endpoints.openapi.model._

class StreamingTest extends WordSpec {

  "ChunkedEndpoint" in {
    val expected =
      PathItem(Map("get" -> Operation(
        None,
        None,
        List(Parameter("file", In.Path, required = true, None, Schema.simpleString)),
        None,
        Map("200" -> Response("", Map("application/octet-stream" -> MediaType(None)))),
        Nil,
        Nil
      )))
    assert(Fixtures.documentation.paths("/assets2/{file}") == expected)
  }

  "WebSocketEndpoint" in {
    val expected =
      PathItem(Map("get" -> Operation(
        None,
        None,
        Nil,
        None,
        Map("101" -> Response("", Map.empty)),
        Nil,
        Nil
      )))
    assert(Fixtures.documentation.paths("/ping-pong") == expected)
  }

}
