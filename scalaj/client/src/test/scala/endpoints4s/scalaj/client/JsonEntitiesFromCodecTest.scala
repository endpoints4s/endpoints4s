package endpoints4s.scalaj.client

import endpoints4s.algebra
import endpoints4s.algebra.circe
import endpoints4s.algebra.client.{JsonFromCodecTestSuite, TextEntitiesTestSuite}

import scala.concurrent.{ExecutionContext, Future}

class TestJsonClient(val address: String)
    extends Endpoints
    with JsonEntitiesFromCodecs
    with circe.JsonEntitiesFromCodecs
    with algebra.JsonFromCodecTestApi
    with algebra.TextEntitiesTestApi
    with circe.JsonFromCirceCodecTestApi {}

class JsonEntitiesFromCodecTest
    extends JsonFromCodecTestSuite[TestJsonClient]
    with TextEntitiesTestSuite[TestJsonClient] {

  val client: TestJsonClient = new TestJsonClient(s"localhost:$stubServerPort")
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] =
    endpoint.callAsync(args)

  jsonFromCodecTestSuite()
  textEntitiesTestSuite()

}
