package endpoints4s.xhr.future

import endpoints4s.algebra
import endpoints4s.xhr.EndpointsSettings
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContextExecutor
import scala.scalajs.concurrent.JSExecutionContext

class XhrClientUrlEncodingTest
    extends AnyWordSpec
    with algebra.client.UrlEncodingTestSuite[TestClient] {

  implicit def executionContext: ExecutionContextExecutor = JSExecutionContext.queue

  val client: TestClient = new TestClient(
    EndpointsSettings().withBaseUri(Some(s"http://localhost:$stubServerPort"))
  )

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

}
