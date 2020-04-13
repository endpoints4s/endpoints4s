package endpoints.algebra.server

import akka.util.ByteString
import endpoints.algebra.TextEntitiesTestApi
import akka.http.scaladsl.model.{
  HttpMethods,
  HttpEntity,
  HttpRequest,
  HttpCharsets,
  ContentTypes,
  MediaTypes
}

trait TextEntitiesTestSuite[T <: TextEntitiesTestApi]
    extends ServerTestBase[T] {

  "TextEntities" should {
    "be forgiving when expecting any text" in {
      serveIdentityEndpoint(serverApi.textRequestEndpointTest) { port =>
        // ContentType: text/plain; charset=UTF-8
        // Request entity is valid UTF-8
        val request1 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/text",
          entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Oekraïene")
        )
        whenReady(send(request1)) {
          case (response, entity) =>
            assert(response.status.intValue() == 200)
            assert(
              response.entity.contentType.mediaType == MediaTypes.`text/plain`
            )
            assert(decodeEntityAsText(response, entity) == "Oekraïene")
        }

        // ContentType: text/plain; charset=UTF-16
        // Request entity is valid UTF-16
        val request2 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/text",
          entity = HttpEntity(
            MediaTypes.`text/plain`.withCharset(HttpCharsets.`UTF-16`),
            "Oekraïene"
          )
        )
        whenReady(send(request2)) {
          case (response, entity) =>
            assert(response.status.intValue() == 200)
            assert(
              response.entity.contentType.mediaType == MediaTypes.`text/plain`
            )
            assert(decodeEntityAsText(response, entity) == "Oekraïene")
        }

        // ContentType: application/javascript; charset=UTF-8
        // Request entity is valid UTF-8 encoded JS string
        val request3 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/text",
          entity = HttpEntity(
            MediaTypes.`application/javascript`
              .withCharset(HttpCharsets.`UTF-8`),
            "var x = 'Oekraïene'"
          )
        )
        whenReady(send(request3)) {
          case (response, entity) =>
            assert(response.status.intValue() == 200)
            assert(
              response.entity.contentType.mediaType == MediaTypes.`text/plain`
            )
            assert(
              decodeEntityAsText(response, entity) == "var x = 'Oekraïene'"
            )
        }

        // ContentType: application/javascript; charset=UTF-16
        // Request entity is valid UTF-16 encoded JS string
        val request4 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/text",
          entity = HttpEntity(
            MediaTypes.`application/javascript`
              .withCharset(HttpCharsets.`UTF-16`),
            "var x = 'Oekraïene'"
          )
        )
        whenReady(send(request4)) {
          case (response, entity) =>
            assert(response.status.intValue() == 200)
            assert(
              response.entity.contentType.mediaType == MediaTypes.`text/plain`
            )
            assert(
              decodeEntityAsText(response, entity) == "var x = 'Oekraïene'"
            )
        }

        // No ContentType header
        // Request entity is valid UTF-8
        val request5 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/text",
          // TODO: switch this back to `Oekraïene` - the leading UTF-8 character is a
          // workaround to avoid https://github.com/playframework/playframework/issues/10181
          entity =
            HttpEntity(ContentTypes.NoContentType, ByteString("Øekraïene"))
        )
        whenReady(send(request5)) {
          case (response, entity) =>
            assert(response.status.intValue() == 200)
            assert(
              response.entity.contentType.mediaType == MediaTypes.`text/plain`
            )
            assert(decodeEntityAsText(response, entity) == "Øekraïene")
        }

        // No ContentType header
        // Request entity is random bytes (not valid UTF-8 encoded string)
        val request6 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/text",
          entity = HttpEntity(
            ContentTypes.NoContentType,
            Array[Byte](0x00.toByte, 0xA0.toByte, 0xBF.toByte)
          )
        )
        whenReady(send(request6)) {
          case (response, entity) =>
            assert(response.status.intValue() == 200)
            assert(
              response.entity.contentType.mediaType == MediaTypes.`text/plain`
            )
        }

        // ContentType: application/octet-stream
        // Request entity is valid UTF-8
        val request7 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/text",
          // TODO: switch this back to `Oekraïene` - the leading UTF-8 character is a
          // workaround to avoid https://github.com/playframework/playframework/issues/10181
          entity = HttpEntity(
            ContentTypes.`application/octet-stream`,
            ByteString("Øekraïene")
          )
        )
        whenReady(send(request7)) {
          case (response, entity) =>
            assert(response.status.intValue() == 200)
            assert(
              response.entity.contentType.mediaType == MediaTypes.`text/plain`
            )
            assert(decodeEntityAsText(response, entity) == "Øekraïene")
        }

        // ContentType: application/octet-stream
        // Request entity is random bytes (not valid UTF-8 encoded string)
        val request8 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/text",
          entity = HttpEntity(
            ContentTypes.`application/octet-stream`,
            Array[Byte](0x00.toByte, 0xA0.toByte, 0xBF.toByte)
          )
        )
        whenReady(send(request8)) {
          case (response, entity) =>
            assert(response.status.intValue() == 200)
            assert(
              response.entity.contentType.mediaType == MediaTypes.`text/plain`
            )
        }
      }
    }

    "be strict when expecting plain text" in {
      serveIdentityEndpoint(serverApi.plainTextRequestEndpointTest) { port =>
        // ContentType: text/plain; charset=UTF-8
        // Request entity is valid UTF-8
        val request1 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/plaintext",
          entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Oekraïene")
        )
        whenReady(send(request1)) {
          case (response, entity) =>
            assert(response.status.intValue() == 200)
            assert(
              response.entity.contentType.mediaType == MediaTypes.`text/plain`
            )
            assert(decodeEntityAsText(response, entity) == "Oekraïene")
        }

        // ContentType: text/plain; charset=UTF-16
        // Request entity is valid UTF-16
        val request2 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/plaintext",
          entity = HttpEntity(
            MediaTypes.`text/plain`.withCharset(HttpCharsets.`UTF-16`),
            "Oekraïene"
          )
        )
        whenReady(send(request2)) {
          case (response, entity) =>
            assert(response.status.intValue() == 200)
            assert(
              response.entity.contentType.mediaType == MediaTypes.`text/plain`
            )
            assert(decodeEntityAsText(response, entity) == "Oekraïene")
        }

        // ContentType: application/javascript; charset=UTF-8
        // Request entity is valid UTF-8 encoded JS string
        val request3 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/plaintext",
          entity = HttpEntity(
            MediaTypes.`application/javascript`
              .withCharset(HttpCharsets.`UTF-8`),
            "var x = 'Oekraïene'"
          )
        )
        whenReady(send(request3)) {
          case (response, entity) =>
            assert(response.status.intValue() == 415)
        }

        // ContentType: application/javascript; charset=UTF-16
        // Request entity is valid UTF-16 encoded JS string
        val request4 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/plaintext",
          entity = HttpEntity(
            MediaTypes.`application/javascript`
              .withCharset(HttpCharsets.`UTF-16`),
            "var x = 'Oekraïene'"
          )
        )
        whenReady(send(request4)) {
          case (response, entity) =>
            assert(response.status.intValue() == 415)
        }

        // No ContentType header
        // Request entity is valid UTF-8
        val request5 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/plaintext",
          entity =
            HttpEntity(ContentTypes.NoContentType, ByteString("Oekraïene"))
        )
        whenReady(send(request5)) {
          case (response, entity) =>
            assert(response.status.intValue() == 415)
        }

        // No ContentType header
        // Request entity is random bytes (not valid UTF-8 encoded string)
        val request6 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/plaintext",
          entity = HttpEntity(
            ContentTypes.NoContentType,
            Array[Byte](0x00.toByte, 0xA0.toByte, 0xBF.toByte)
          )
        )
        whenReady(send(request6)) {
          case (response, entity) =>
            assert(response.status.intValue() == 415)
        }

        // ContentType: application/octet-stream
        // Request entity is valid UTF-8
        val request7 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/plaintext",
          entity = HttpEntity(
            ContentTypes.`application/octet-stream`,
            ByteString("Oekraïene")
          )
        )
        whenReady(send(request7)) {
          case (response, entity) =>
            assert(response.status.intValue() == 415)
        }

        // ContentType: application/octet-stream
        // Request entity is random bytes (not valid UTF-8 encoded string)
        val request8 = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/plaintext",
          entity = HttpEntity(
            ContentTypes.`application/octet-stream`,
            Array[Byte](0x00.toByte, 0xA0.toByte, 0xBF.toByte)
          )
        )
        whenReady(send(request8)) {
          case (response, entity) =>
            assert(response.status.intValue() == 415)
        }
      }
    }
  }
}
