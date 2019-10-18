package endpoints.akkahttp.server

import akka.http.scaladsl.model.StatusCodes.{BadRequest, Forbidden, InternalServerError, OK, Unauthorized}
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import endpoints.algebra
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future

/* defines the common api to implement */
trait EndpointsTestApi extends Endpoints
  with BasicAuthentication
  with algebra.BasicAuthTestApi
  with algebra.EndpointsTestApi

/* implements the endpoint using an akka-based custom json handling */
class EndpointsEntitiesTestApi extends EndpointsTestApi
  with JsonEntities

class EndpointsTest extends WordSpec with Matchers with ScalatestRouteTest {

  object TestRoutes extends EndpointsEntitiesTestApi {

    val singleStaticGetSegment = endpoint[Unit, Unit](
      get(path / "segment1"),
      (_: Unit) => complete("Ok")
    ).implementedBy(_ => ())

    val protectedEndpointRoute =
      protectedEndpoint.implementedBy(credentials => if (credentials.username == "admin") Some("Hello!") else None)

    val protectedEndpointWithParameterRoute =
      protectedEndpointWithParameter.implementedBy { case (id, credentials) =>
        if (credentials.username == "admin") Some(s"Requested user $id") else None
      }

    val smokeEndpointSyncRoute =
      smokeEndpoint.implementedBy(_ => sys.error("Sorry."))

    val smokeEndpointAsyncRoute =
      smokeEndpoint.implementedByAsync(_ => Future.failed(new Exception("Sorry.")))

  }

  "Single segment route" should {

    "match single segment request" in {
      // tests:
      Get("/segment1") ~> TestRoutes.singleStaticGetSegment ~> check {
        responseAs[String] shouldEqual "Ok"
      }
    }
    "leave GET requests to other paths unhandled" in {
      Get("/segment1/segment2") ~> TestRoutes.singleStaticGetSegment ~> check {
        handled shouldBe false
      }
    }

  }

  "Authenticated routes" should {

    "reject unauthenticated requests" in {
      Get("/users") ~> TestRoutes.protectedEndpointRoute ~> check {
        handled shouldBe true
        status shouldBe Unauthorized
        header("www-authenticate").map(_.value()) shouldBe Some("Basic realm=\"Realm\",charset=UTF-8")
        responseAs[String] shouldBe ""
      }
    }

    "accept authenticated requests" in {
      Get("/users").withHeaders(Authorization(BasicHttpCredentials("admin", "foo"))) ~> TestRoutes.protectedEndpointRoute ~> check {
        handled shouldBe true
        status shouldBe OK
        responseAs[String] shouldBe "Hello!"
      }
    }

    "forbid authenticated requests with insufficient rights" in {
      Get("/users").withHeaders(Authorization(BasicHttpCredentials("alice", "foo"))) ~> TestRoutes.protectedEndpointRoute ~> check {
        handled shouldBe true
        status shouldBe Forbidden
        responseAs[String] shouldBe ""
      }
    }

    "reject unauthenticated requests with invalid parameters before handling authorization" in {
      Get("/users/foo") ~> TestRoutes.protectedEndpointWithParameterRoute ~> check {
        handled shouldBe true
        status shouldBe BadRequest
        responseAs[String] shouldBe "[\"Invalid integer value 'foo' for segment 'id'\"]"
      }
    }

  }

  "Routes" should {

    "Handle exceptions by default" in {
      Get("/user/foo/description?name=a&age=1") ~> TestRoutes.smokeEndpointSyncRoute ~> check {
        handled shouldBe true
        status shouldBe InternalServerError
        responseAs[String] shouldBe "[\"Sorry.\"]"
      }
      Get("/user/foo/description?name=a&age=1") ~> TestRoutes.smokeEndpointAsyncRoute ~> check {
        handled shouldBe true
        status shouldBe InternalServerError
        responseAs[String] shouldBe "[\"Sorry.\"]"
      }
    }

  }

}
