package endpoints4s.algebra.server

import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest}
import endpoints4s.algebra
import endpoints4s.algebra.User

trait JsonEntitiesFromSchemasTestSuite[
    T <: algebra.JsonEntitiesFromSchemasTestApi
] extends ServerTestBase[T] {

  "Single segment route" should {

    "match single segment request" in {
      serveEndpoint(serverApi.singleStaticGetSegment, User("Alice", 42)) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/user")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          assert(response.status.intValue() == 200)
          ujson.read(entity) shouldEqual ujson.Obj(
            "name" -> ujson.Str("Alice"),
            "age" -> ujson.Num(42)
          )
        }
        ()
      }
    }

    "leave GET requests to other paths unhandled" in {
      serveEndpoint(serverApi.singleStaticGetSegment, User("Alice", 42)) { port =>
        val request =
          HttpRequest(uri = s"http://localhost:$port/user/profile")
        whenReady(send(request)) { case (response, entity) =>
          assert(response.status.intValue() == 404)
        }
        ()
      }
    }
  }

  "JSON entities" should {

    "validate query parameters and headers before validating the entity" in {
      def request(path: String, jsonEntity: String) =
        HttpRequest(HttpMethods.PUT, path)
          .withEntity(ContentTypes.`application/json`, jsonEntity)

      serveEndpoint(serverApi.updateUser, User("Alice", 55)) { port =>
        // Invalid URL and entity
        whenReady(
          sendAndDecodeEntityAsText(
            request(
              s"http://localhost:$port/user/foo",
              "{\"name\":\"Alice\",\"age\":true}"
            )
          )
        ) { case (response, entity) =>
          assert(response.status.intValue() == 400)
          ujson.read(entity) shouldBe ujson.Arr(
            ujson.Str("Invalid integer value 'foo' for segment 'id'")
          )
        }

        // Valid URL and invalid entity
        whenReady(
          sendAndDecodeEntityAsText(
            request(
              s"http://localhost:$port/user/42",
              "something that is not JSON"
            )
          )
        ) { case (response, entity) =>
          assert(response.status.intValue() == 400)
          ujson.read(entity) shouldBe ujson.Arr(
            ujson.Str("Invalid JSON document")
          )
        }

        // Valid URL and invalid entity (2)
        whenReady(
          sendAndDecodeEntityAsText(
            request(
              s"http://localhost:$port/user/42",
              "{\"name\":\"Alice\",\"age\":true}"
            )
          )
        ) { case (response, entity) =>
          assert(response.status.intValue() == 400)
          ujson.read(entity) shouldBe ujson.Arr(
            ujson.Str("Invalid integer value: true")
          )
        }

        // Valid URL and invalid entity (3)
        whenReady(
          sendAndDecodeEntityAsText(
            HttpRequest(HttpMethods.PUT, s"http://localhost:$port/user/42")
              .withEntity(
                ContentTypes.`text/plain(UTF-8)`,
                "{\"name\":\"Alice\",\"age\":55}"
              )
          )
        ) { case (response, entity) =>
          assert(response.status.intValue() == 415)
        }

        // Valid URL and entity
        whenReady(
          sendAndDecodeEntityAsText(
            request(
              s"http://localhost:$port/user/42",
              "{\"name\":\"Alice\",\"age\":55}"
            )
          )
        ) { case (response, entity) =>
          assert(response.status.intValue() == 200)
          assert(
            response.entity.contentType == ContentTypes.`application/json`
          )
          ujson.read(entity) shouldBe ujson.Obj(
            "name" -> ujson.Str("Alice"),
            "age" -> ujson.Num(55)
          )
        }
        ()
      }

    }

  }

}
