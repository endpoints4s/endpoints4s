package endpoints4s.akkahttp.server

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes.{BadRequest, OK}
import akka.http.scaladsl.server.UnsupportedRequestContentTypeRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import endpoints4s.algebra.User
import endpoints4s.generic
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EndpointsJsonSchemaTestApi
    extends Endpoints
    with generic.JsonSchemas
    with JsonEntitiesFromSchemas

class EndpointsJsonSchemaTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  object testRoutes extends EndpointsJsonSchemaTestApi {

    implicit val userJsonSchema: JsonSchema[User] = genericJsonSchema[User]

    val singleStaticGetSegment = endpoint[Unit, User](
      get(path / "user"),
      ok(jsonResponse[User])
    ).implementedBy(_ => User("Bob", 30))

    val updateUser =
      endpoint(
        put(path / "user" / segment[Long]("id"), jsonRequest[User]),
        ok(jsonResponse[User])
      ).implementedBy { case (_, user) => user }
  }

  "Single segment route" should {

    "match single segment request" in {
      Get("/user") ~> testRoutes.singleStaticGetSegment ~> check {
        ujson.read(responseAs[String]) shouldEqual ujson.Obj(
          "name" -> ujson.Str("Bob"),
          "age" -> ujson.Num(30)
        )
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/user/profile") ~> testRoutes.singleStaticGetSegment ~> check {
        handled shouldBe false
      }
    }
  }

  "JSON entities" should {

    "validate query parameters and headers before validating the entity" in {
      def request(path: String, jsonEntity: String) =
        Put(path)
          .withEntity(ContentTypes.`application/json`, jsonEntity)

      // Invalid URL and entity
      request("/user/foo", "{\"name\":\"Alice\",\"age\":true}") ~> testRoutes.updateUser ~> check {
        handled shouldBe true
        status shouldBe BadRequest
        ujson.read(responseAs[String]) shouldBe ujson.Arr(
          ujson.Str("Invalid integer value 'foo' for segment 'id'")
        )
      }

      // Valid URL and invalid entity
      request("/user/42", "something that is not JSON") ~> testRoutes.updateUser ~> check {
        handled shouldBe true
        status shouldBe BadRequest
        ujson.read(responseAs[String]) shouldBe ujson.Arr(
          ujson.Str("Invalid JSON document")
        )
      }

      // Valid URL and invalid entity (2)
      request("/user/42", "{\"name\":\"Alice\",\"age\":true}") ~> testRoutes.updateUser ~> check {
        handled shouldBe true
        status shouldBe BadRequest
        ujson.read(responseAs[String]) shouldBe ujson.Arr(
          ujson.Str("Invalid integer value: true")
        )
      }

      // Valid URL and invalid entity (3)
      Put("/user/42").withEntity(
        ContentTypes.`text/plain(UTF-8)`,
        "{\"name\":\"Alice\",\"age\":true}"
      ) ~> testRoutes.updateUser ~> check {
        handled shouldBe false
        rejection shouldBe UnsupportedRequestContentTypeRejection(
          supported = Set(ContentTypes.`application/json`),
          contentType = Some(ContentTypes.`text/plain(UTF-8)`)
        )
      }

      // Valid URL and entity
      request("/user/42", "{\"name\":\"Alice\",\"age\":55}") ~> testRoutes.updateUser ~> check {
        handled shouldBe true
        status shouldBe OK
        contentType shouldBe ContentTypes.`application/json`
        ujson.read(responseAs[String]) shouldBe ujson.Obj(
          "name" -> ujson.Str("Alice"),
          "age" -> ujson.Num(55)
        )
      }
    }

  }

}
