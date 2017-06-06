package endpoints.akkahttp.server

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}
import scala.language.reflectiveCalls

/**
  * Created by wpitula on 2/26/17.
  */
class EndpointsTest extends WordSpec with Matchers with ScalatestRouteTest {

  val testRoutes = new Endpoints {
    val singleStaticGetSegment = endpoint[Unit, Unit](
      get[Unit, Unit](path / "segment1"),
      _ => complete("Ok")
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
