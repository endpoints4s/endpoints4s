package endpoints.scalaj.client

import endpoints.algebra.CirceEntities.CirceCodec
import endpoints.testsuite.JsonTestApi
import endpoints.testsuite.User
import endpoints.testsuite.Address
import endpoints.testsuite.client.JsonTestSuite
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class TestClient(val address: String ) extends JsonTestApi with Endpoints with CirceEntities {

  import io.circe.generic.auto._

  implicit def userCodec: CirceCodec[User] = CirceCodec.fromEncoderAndDecoder[User]

  implicit def addresCodec: CirceCodec[Address] = CirceCodec.fromEncoderAndDecoder[Address]

}

class CirceEntitiesTest extends JsonTestSuite[TestClient] {

  val client: TestClient = new TestClient(s"localhost:$wiremockPort")

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] =
    endpoint.callAsync(args)

  clientTestSuite()

}
