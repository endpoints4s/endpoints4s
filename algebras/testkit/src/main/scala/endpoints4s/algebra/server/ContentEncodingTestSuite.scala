package endpoints4s.algebra.server

import org.apache.pekko.http.scaladsl.coding.Coders.Gzip
import org.apache.pekko.http.scaladsl.model.headers.HttpEncodings.gzip
import org.apache.pekko.http.scaladsl.model.headers.{HttpEncoding, `Accept-Encoding`, `Content-Encoding`}
import org.apache.pekko.http.scaladsl.model.{HttpRequest, StatusCodes}
import endpoints4s.algebra.EndpointsTestApi

trait ContentEncodingTestSuite[T <: EndpointsTestApi] extends EndpointsTestSuite[T] {

  "ContentEncoding" should {
    "be set according to request’s Accept-Encoding header" in {
      serveEndpoint(serverApi.smokeEndpoint, "response") { port =>
        val basicRequest =
          HttpRequest(uri = s"http://localhost:$port/user/abcd/description?name=a&age=0")

        // No Accept-Encoding: response is not encoded
        whenReady(send(basicRequest)) { case (response, entity) =>
          response.status shouldBe StatusCodes.OK
          assert(response.headers[`Content-Encoding`].isEmpty)
          entity.utf8String shouldBe "response"
        }

        val acceptGzipRequest = basicRequest.withHeaders(`Accept-Encoding`(gzip))
        // Accept gzip: response is compressed
        whenReady(send(acceptGzipRequest)) { case (response, entity) =>
          response.status shouldBe StatusCodes.OK
          assert(response.headers[`Content-Encoding`].contains(`Content-Encoding`(gzip)))
          whenReady(Gzip.decode(entity)) { decodedEntity =>
            decodedEntity.utf8String shouldBe "response"
          }
        }

        val acceptCustomEncodingRequest =
          basicRequest.withHeaders(`Accept-Encoding`(HttpEncoding.custom("dummy")))
        // Custom Accept-Encoding: no error, response is not encoded.
        whenReady(send(acceptCustomEncodingRequest)) { case (response, entity) =>
          response.status shouldBe StatusCodes.OK
          assert(response.headers[`Content-Encoding`].isEmpty)
          assert(entity.utf8String == "response")
        }

        ()
      }
    }
  }

}
