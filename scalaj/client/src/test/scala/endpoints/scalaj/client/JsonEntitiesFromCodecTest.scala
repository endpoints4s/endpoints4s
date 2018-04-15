package endpoints.scalaj.client


//FIXME
//class TestJsonClient(val address: String ) extends JsonTestApi with Endpoints with JsonEntitiesFromCodec with circe.JsonEntitiesFromCodec {
//
//  implicit def userCodec: Codec[String, User] = jsonCodec[User]
//
//  implicit def addresCodec: Codec[String, Address] = jsonCodec[Address]
//
//}
//
//class JsonEntitiesFromCodecTest extends JsonTestSuite[TestJsonClient] {
//
//  val client: TestJsonClient = new TestJsonClient(s"localhost:$wiremockPort")
//
//  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] =
//    endpoint.callAsync(args)
//
//  clientTestSuite()
//
//}
