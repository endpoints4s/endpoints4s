package endpoints.akkahttp.server.playjson

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes.{BadRequest, OK}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import endpoints.akkahttp.server.Endpoints
import endpoints.algebra.User
import endpoints.generic
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsNumber, JsString, JsValue, Json}

import scala.language.reflectiveCalls

class EndpointsJsonSchemaTestApi
  extends Endpoints
  with generic.JsonSchemas
  with JsonSchemaEntities


class EndpointsJsonSchemaTest extends WordSpec with Matchers with ScalatestRouteTest with de.heikoseeberger.akkahttpplayjson.PlayJsonSupport{

  val testRoutes = new EndpointsJsonSchemaTestApi {

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

  "Single segment route" should {

    import play.api.libs.json.{OFormat, Json}
    implicit val userJson: OFormat[User] = Json.format[User]

    "match single segment request" in {
      Get("/user") ~> testRoutes.singleStaticGetSegment ~> check {
        responseAs[User] shouldEqual User("Bob", 30)
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
        responseAs[JsValue] shouldBe Json.arr(JsString("Invalid integer value 'foo' for segment 'id'"))
      }

      // Valid URL and invalid entity
      request("/user/42", "{\"name\":\"Alice\",\"age\":true}") ~> testRoutes.updateUser ~> check {
        handled shouldBe true
        status shouldBe BadRequest
        responseAs[JsValue] shouldBe Json.arr(JsString("error.expected.jsnumber for obj.age"))
      }

      // Valid URL and entity
      request("/user/42", "{\"name\":\"Alice\",\"age\":55}") ~> testRoutes.updateUser ~> check {
        handled shouldBe true
        status shouldBe OK
        responseAs[JsValue] shouldBe Json.obj("name" -> JsString("Alice"), "age" -> JsNumber(55))
      }
    }

  }

}
