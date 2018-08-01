package endpoints.scalaj.client

import endpoints.algebra
import endpoints.algebra.circe
import endpoints.algebra.client.JsonFromCodecTestSuite

import scala.concurrent.{ExecutionContext, Future}


class TestJsonClient(val _address: String )
  extends algebra.JsonFromCodecTestApi
    with circe.JsonFromCirceCodecTestApi {
  override val entities: JsonEntitiesFromCodec with circe.JsonEntitiesFromCodec = new JsonEntitiesFromCodec with circe.JsonEntitiesFromCodec {
    override val endpoints: Endpoints = new Endpoints {
      override val requests: Requests = new Requests {
        override val urls: Urls = new Urls {
          override def address: String = _address
        }
        override val methods: Methods = new Methods {}
      }
      override val responses: Responses = new Responses {}
    }
  }
}

class JsonEntitiesFromCodecTest extends JsonFromCodecTestSuite {

  override val api: TestJsonClient = new TestJsonClient(s"localhost:$wiremockPort")
  implicit val ec = ExecutionContext.Implicits.global

  def call[Req, Resp](endpoint: Endpoint[Req, Resp], args: Req): Future[Resp] =
    endpoint.asInstanceOf[api.entities.endpoints.Endpoint[Req, Resp]].callAsync(args)

  jsonFromCodecTestSuite()

}
