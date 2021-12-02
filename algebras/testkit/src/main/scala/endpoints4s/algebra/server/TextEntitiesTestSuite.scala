package endpoints4s.algebra.server

import endpoints4s.algebra.TextEntitiesTestApi
import akka.http.scaladsl.model.{
  HttpMethods,
  HttpEntity,
  HttpRequest,
  HttpCharsets,
  ContentTypes,
  MediaTypes
}

trait TextEntitiesTestSuite[T <: TextEntitiesTestApi] extends ServerTestBase[T] {

  "TextEntities" should {
    "accept `text/plain` requests with UTF-8 encoding" in {
      serveIdentityEndpoint(serverApi.textRequestEndpointTest) { port =>
        val request = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/text",
          entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Oekraïene")
        )
        whenReady(send(request)) { case (response, entity) =>
          assert(response.status.intValue() == 200)
          assert(
            response.entity.contentType.mediaType == MediaTypes.`text/plain`
          )
          assert(response.entity.contentType.charsetOption.nonEmpty)
          assert(decodeEntityAsText(response, entity) == "Oekraïene")
          ()
        }
      }
    }

    "accept `text/plain` requests with UTF-16 encoding" in {
      serveIdentityEndpoint(serverApi.textRequestEndpointTest) { port =>
        val request = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/text",
          entity = HttpEntity(
            MediaTypes.`text/plain`.withCharset(HttpCharsets.`UTF-16`),
            "Oekraïene"
          )
        )
        whenReady(send(request)) { case (response, entity) =>
          assert(response.status.intValue() == 200)
          assert(
            response.entity.contentType.mediaType == MediaTypes.`text/plain`
          )
          assert(response.entity.contentType.charsetOption.nonEmpty)
          assert(decodeEntityAsText(response, entity) == "Oekraïene")
          ()
        }
      }
    }

    "reject non-`text/plain` requests" in {
      serveIdentityEndpoint(serverApi.textRequestEndpointTest) { port =>
        val request = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/text",
          entity = HttpEntity(
            MediaTypes.`application/javascript`
              .withCharset(HttpCharsets.`UTF-8`),
            "var x = 'Oekraïene'"
          )
        )
        whenReady(send(request)) { case (response, entity) =>
          assert(response.status.intValue() == 415)
          ()
        }
      }
    }
  }
}
