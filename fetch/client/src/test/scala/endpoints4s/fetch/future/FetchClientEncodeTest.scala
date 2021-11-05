package endpoints4s.fetch.future

import endpoints4s.algebra
import endpoints4s.fetch.EndpointsSettings
import org.scalatest.wordspec.AnyWordSpec

import scala.scalajs.concurrent.JSExecutionContext

class FetchClientEncodeTest extends AnyWordSpec with algebra.client.EncodeTestSuite[TestClient] {

  implicit def executionContext = JSExecutionContext.queue

  val client: TestClient = new TestClient(EndpointsSettings(Some("http://localhost:8080")))

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)
}
