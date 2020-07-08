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

  val client: TestJsonClient = new TestJsonClient(s"localhost:$wiremockPort")
  implicit val ec = ExecutionContext.Implicits.global

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] =
    endpoint.callAsync(args)

  def encodeUrl[A](url: client.Url[A])(a: A): String = {
    val req = url.toReq(a)
    val encodedUrl = req.urlBuilder(req)
    val pathAndQuery =
      encodedUrl.drop(
        s"http://localhost:$wiremockPort".size
      ) // Remove scheme, host and port from URL
    if (pathAndQuery.startsWith("/?") || pathAndQuery == "/")
      pathAndQuery.drop(
        1
      ) // For some reason, scalaj always inserts a slash when the path is empty
    else pathAndQuery
  }

  jsonFromCodecTestSuite()
  textEntitiesTestSuite()

}
