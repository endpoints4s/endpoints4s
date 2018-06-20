package endpoints.scalaj.client

import endpoints.algebra
import endpoints.algebra.circe
import endpoints.algebra.client.JsonFromCodecTestSuite

import scala.concurrent.{ExecutionContext, Future}


class TestJsonClient(val address: String )
  extends Endpoints
    with JsonEntitiesFromCodec
    with circe.JsonEntitiesFromCodec
    with algebra.JsonFromCodecTestApi
    with circe.JsonFromCirceCodecTestApi {


}

class JsonEntitiesFromCodecTest extends JsonFromCodecTestSuite[TestJsonClient] {

  val client: TestJsonClient = new TestJsonClient(s"localhost:$wiremockPort")
  implicit val ec = ExecutionContext.Implicits.global

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] =
    endpoint.callAsync(args)

  jsonFromCodecTestSuite()

}
