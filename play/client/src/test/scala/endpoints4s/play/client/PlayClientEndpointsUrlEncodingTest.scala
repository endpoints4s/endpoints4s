package endpoints4s.play.client

import endpoints4s.algebra
import org.scalatest.BeforeAndAfterAll
import play.api.test.WsTestClient

import scala.concurrent.ExecutionContext

class PlayClientEndpointsUrlEncodingTest
    extends algebra.client.UrlEncodingTestSuite[TestClient]
    with BeforeAndAfterAll {

  import ExecutionContext.Implicits.global

  val wsClient = new WsTestClient.InternalWSClient("http", 8080)
  val client: TestClient =
    new TestClient(s"http://localhost:8080", wsClient)

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

  override protected def afterAll(): Unit = {
    wsClient.close()
    super.afterAll()
  }
}
