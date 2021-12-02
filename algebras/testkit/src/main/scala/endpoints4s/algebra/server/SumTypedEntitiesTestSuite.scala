package endpoints4s.algebra.server

import akka.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest}
import endpoints4s.algebra

trait SumTypedEntitiesTestSuite[
    T <: algebra.SumTypedEntitiesTestApi
] extends ServerTestBase[T] {

  "Sum typed route" should {

    "handle `text/plain` content-type requests" in {
      serveIdentityEndpoint(serverApi.sumTypedEndpoint) { port =>
        val request =
          HttpRequest(HttpMethods.POST, s"http://localhost:$port/user-or-name")
            .withEntity(ContentTypes.`text/plain(UTF-8)`, "Alice")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          assert(response.status.intValue() == 200)
          assert(
            response.entity.contentType == ContentTypes.`text/plain(UTF-8)`
          )
          entity shouldEqual "Alice"
        }
        ()
      }
    }

    "handle `application/json` content-type requests" in {
      serveIdentityEndpoint(serverApi.sumTypedEndpoint) { port =>
        val request =
          HttpRequest(HttpMethods.POST, s"http://localhost:$port/user-or-name")
            .withEntity(
              ContentTypes.`application/json`,
              "{\"name\":\"Alice\",\"age\":42}"
            )
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          assert(response.status.intValue() == 200)
          assert(
            response.entity.contentType == ContentTypes.`application/json`
          )
          ujson.read(entity) shouldEqual ujson.Obj(
            "name" -> ujson.Str("Alice"),
            "age" -> ujson.Num(42)
          )
        }
        ()
      }
    }

    "handle `application/json` content-type requests with malformed bodies" in {
      serveIdentityEndpoint(serverApi.sumTypedEndpoint) { port =>
        val request =
          HttpRequest(HttpMethods.POST, s"http://localhost:$port/user-or-name")
            .withEntity(
              ContentTypes.`application/json`,
              "{\"name\":\"Alice\"}"
            )
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          assert(response.status.intValue() == 400)
        }
        ()
      }

    }

    "not handle `application/x-www-form-urlencoded` content-type requests" in {
      serveIdentityEndpoint(serverApi.sumTypedEndpoint) { port =>
        val request =
          HttpRequest(HttpMethods.POST, s"http://localhost:$port/user-or-name")
            .withEntity(
              ContentTypes.`application/x-www-form-urlencoded`,
              "name=Alice&age=42"
            )
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, _) =>
          assert(response.status.intValue() == 415)
        }
        ()
      }
    }
  }
}
