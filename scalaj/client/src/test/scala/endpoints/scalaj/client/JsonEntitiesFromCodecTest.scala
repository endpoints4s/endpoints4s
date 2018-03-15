package endpoints.scalaj.client

import endpoints.algebra.{Codec, circe}
import endpoints.testsuite.client.JsonTestSuite
import endpoints.testsuite.{Address, JsonTestApi, User}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestJsonClient(val address: String ) extends JsonTestApi with Endpoints with JsonEntitiesFromCodec with circe.JsonEntitiesFromCodec {

  implicit def userCodec: Codec[String, User] = jsonCodec[User]

  implicit def addresCodec: Codec[String, Address] = jsonCodec[Address]

}

class JsonEntitiesFromCodecTest extends JsonTestSuite[TestJsonClient] {

  val client: TestJsonClient = new TestJsonClient(s"localhost:$wiremockPort")

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] =
    endpoint.callAsync(args)

  clientTestSuite()

}
