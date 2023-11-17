package endpoints4s.sttp.client

import endpoints4s.algebra
import org.scalatest.BeforeAndAfterAll
import sttp.client3.pekkohttp.PekkoHttpBackend

import scala.concurrent.Future

class SttpEndpointsUrlEncodingTest
    extends algebra.client.UrlEncodingTestSuite[TestClient[Future]]
    with BeforeAndAfterAll {

  val backend = PekkoHttpBackend()

  val client: TestClient[Future] =
    new TestClient(s"http://localhost:$stubServerPortHTTP", backend)

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

  override def afterAll(): Unit = {
    backend.close()
    Thread.sleep(1000) // See https://github.com/softwaremill/sttp/issues/269
    super.afterAll()
  }
}
