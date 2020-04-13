package endpoints.akkahttp.server

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import endpoints.algebra

import scala.concurrent.Future
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/* defines the common api to implement */
trait EndpointsTestApi
    extends Endpoints
    with algebra.EndpointsTestApi
    with algebra.TextEntitiesTestApi

/* implements the endpoint using an akka-based custom json handling */
class EndpointsEntitiesTestApi extends EndpointsTestApi with JsonEntities

class EndpointsTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  object TestRoutes extends EndpointsEntitiesTestApi {

    val singleStaticGetSegment = endpoint[Unit, Unit](
      get(path / "segment1"),
      (_: Unit) => complete("Ok")
    ).implementedBy(_ => ())

    val smokeEndpointSyncRoute =
      smokeEndpoint.implementedBy(_ => sys.error("Sorry."))

    val smokeEndpointAsyncRoute =
      smokeEndpoint.implementedByAsync(_ =>
        Future.failed(new Exception("Sorry."))
      )

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

  "Routes" should {

    "Handle exceptions by default" in {
      Get("/user/foo/description?name=a&age=1") ~> TestRoutes.smokeEndpointAsyncRoute ~> check {
        handled shouldBe true
        status shouldBe InternalServerError
        responseAs[String] shouldBe "[\"Sorry.\"]"
      }
    }

  }

}
