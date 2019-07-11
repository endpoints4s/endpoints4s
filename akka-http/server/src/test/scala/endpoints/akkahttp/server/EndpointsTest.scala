package endpoints.akkahttp.server

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

import scala.language.reflectiveCalls
import endpoints.algebra
import endpoints.algebra.{JsonFromCodecTestApi, Http1JsonStreamingTestApi, circe}

/* defines the common api to implement */
trait EndpointsTestApi extends Endpoints
  with BasicAuthentication
  with algebra.BasicAuthTestApi
  with algebra.EndpointsTestApi

/* implements the endpoint using a codecs-based json handling */
class EndpointsCodecsTestApi(val settings: Http1Streaming.Settings) extends EndpointsTestApi
  with JsonFromCodecTestApi
  with circe.JsonFromCirceCodecTestApi
  with JsonEntitiesFromCodec
  with circe.JsonEntitiesFromCodec
  with Http1JsonStreamingTestApi
  with circe.Http1JsonStreamingTestApi
  with Http1JsonStreaming

/* implements the endpoint using an akka-based custom json handling */
class EndpointsEntitiesTestApi extends EndpointsTestApi
  with JsonEntities

//TODO use EndpointsApi from algebra tests
class EndpointsTest extends WordSpec with Matchers with ScalatestRouteTest {

  val testRoutes = new Endpoints {
    val singleStaticGetSegment = endpoint[Unit, Unit](
      get(path / "segment1"),
      (_: Unit) => complete("Ok")
    ).implementedBy(_ => ())
  }

  "Single segment route" should {

    "match single segment request" in {
      // tests:
      Get("/segment1") ~> testRoutes.singleStaticGetSegment ~> check {
        responseAs[String] shouldEqual "Ok"
      }
    }
    "leave GET requests to other paths unhandled" in {
      Get("/segment1/segment2") ~> testRoutes.singleStaticGetSegment ~> check {
        handled shouldBe false
      }
    }

  }
}
