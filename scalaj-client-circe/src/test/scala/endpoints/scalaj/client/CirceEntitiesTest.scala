package endpoints.scalaj.client

import endpoints.algebra.CirceEntities.CirceCodec
import endpoints.testsuite.JsonTestApi
import endpoints.testsuite.User
import endpoints.testsuite.Address
import endpoints.testsuite.client.JsonTestSuite
import org.scalatest.WordSpec

class TestClient(val address: String ) extends JsonTestApi with Endpoints with CirceEntities {

  import io.circe.generic.auto._

  override implicit def userCodec: CirceCodec[User] = CirceCodec.fromEncoderAndDecoder[User]

  override implicit def addresCodec: CirceCodec[Address] = CirceCodec.fromEncoderAndDecoder[Address]

}

class CirceEntitiesTest extends JsonTestSuite[TestClient] {


  override val client: TestClient = new TestClient(s"localhost:$wiremockPort")

  override def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Resp = endpoint.callUnsafe(args)

  clientTestSuite()


}
