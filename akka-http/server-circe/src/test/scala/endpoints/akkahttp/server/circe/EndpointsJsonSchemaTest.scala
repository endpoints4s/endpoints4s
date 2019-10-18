package endpoints.akkahttp.server.circe

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes.{BadRequest, OK}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import endpoints.akkahttp.server.Endpoints
import endpoints.algebra.User
import endpoints.generic
import io.circe.Json
import org.scalatest.{Matchers, WordSpec}

class EndpointsJsonSchemaTestApi
  extends Endpoints
  with generic.JsonSchemas
  with JsonSchemaEntities


class EndpointsJsonSchemaTest extends WordSpec with Matchers with ScalatestRouteTest {

  object TestRoutes extends EndpointsJsonSchemaTestApi {

    implicit val userJsonSchema: JsonSchema[User] = genericJsonSchema[User]

    val singleStaticGetSegment = endpoint[Unit, User](
      get(path / "user"), ok(jsonResponse[User])
    ).implementedBy(_ => User("Bob", 30))

    val updateUser =
      endpoint(
        put(path / "user" / segment[Long]("id"), jsonRequest[User]),
        ok(jsonResponse[User])
      ).implementedBy { case (_, user) => user }
  }

  import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
  import TestRoutes.JsonSchema.toCirceDecoder
  import TestRoutes.userJsonSchema

  "Single segment route" should {

    "match single segment request" in {
      Get("/user") ~> TestRoutes.singleStaticGetSegment ~> check {
        responseAs[User] shouldEqual User("Bob", 30)
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/user/profile") ~> TestRoutes.singleStaticGetSegment ~> check {
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
      request("/user/foo", "{\"name\":\"Alice\",\"age\":true}") ~> TestRoutes.updateUser ~> check {
        handled shouldBe true
        status shouldBe BadRequest
        responseAs[Json] shouldBe Json.arr(Json.fromString("Invalid integer value 'foo' for segment 'id'"))
      }

      // Valid URL and invalid entity
      request("/user/42", "{\"name\":\"Alice\",\"age\":true}") ~> TestRoutes.updateUser ~> check {
        handled shouldBe true
        status shouldBe BadRequest
        responseAs[Json] shouldBe Json.arr(Json.fromString("DecodingFailure at .age: Int"))
      }

      // Valid URL and entity
      request("/user/42", "{\"name\":\"Alice\",\"age\":55}") ~> TestRoutes.updateUser ~> check {
        handled shouldBe true
        status shouldBe OK
        responseAs[Json] shouldBe Json.obj("name" -> Json.fromString("Alice"), "age" -> Json.fromInt(55))
      }
    }

  }
}
