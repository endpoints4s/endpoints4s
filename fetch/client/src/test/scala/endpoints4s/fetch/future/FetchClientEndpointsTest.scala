package endpoints4s.fetch.future

import java.util.UUID

import endpoints4s.algebra
import endpoints4s.algebra.ChunkedJsonEntitiesTestApi
import endpoints4s.fetch.ChunkedJsonEntities
import endpoints4s.fetch.EndpointsSettings
import endpoints4s.fetch.JsonEntitiesFromCodecs
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext

class TestClient(val endpointsSettings: EndpointsSettings)(implicit val ec: ExecutionContext)
    extends Endpoints
    with algebra.EndpointsTestApi
    with algebra.TextEntitiesTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.SumTypedEntitiesTestApi
    with algebra.circe.JsonFromCirceCodecTestApi
    with JsonEntitiesFromCodecs
    with algebra.circe.JsonEntitiesFromCodecs
//    with ChunkedJsonEntities
//    with ChunkedJsonEntitiesTestApi
//    with algebra.circe.ChunkedJsonEntitiesTestApi

//TODO needs Scala.js test setup
class FetchClientEndpointsTest extends AsyncWordSpec with Matchers {

  implicit override def executionContext = JSExecutionContext.queue

  private def withClient(block: TestClient => Future[Assertion]) = {
    val client: TestClient = new TestClient(EndpointsSettings(Some("http://localhost:8080")))
    block(client)
  }

  "Client interpreted" should {

    "return server response for UUID" in withClient { client =>
      val uuid = UUID.fromString("f3ac9be0-6339-4650-afb6-7305ece8edce")
      val response = "wiremockeResponse"

      client
        .UUIDEndpoint(uuid, "name1", 18)
        .map(_ shouldEqual response)
    }
  }
}
