package endpoints.akkahttp.server

import akka.http.scaladsl.testkit.ScalatestRouteTest
import endpoints.algebra.User
import endpoints.generic
import org.scalatest.{Matchers, WordSpec}

import scala.language.reflectiveCalls

class EndpointsJsonSchemaTestApi
  extends Endpoints
  with generic.JsonSchemas
  with JsonSchemaEntities


class EndpointsJsonSchemaTest extends WordSpec with Matchers with ScalatestRouteTest {

  val testRoutes = new EndpointsJsonSchemaTestApi {

    implicit val userJsonSchema: JsonSchema[User] = genericJsonSchema[User]

    val singleStaticGetSegment = endpoint[Unit, User](
      get(path / "user"), jsonResponse[User]()
    ).implementedBy(_ => User("Bob", 30))
  }

  "Single segment route" should {

    import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
    import io.circe.generic.auto._

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
}
