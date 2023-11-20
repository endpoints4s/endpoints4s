package endpoints4s.pekkohttp.client

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.TestKit
import endpoints4s.algebra
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.ExecutionContext

class PekkoHttpClientUrlEncodingTest
    extends algebra.client.UrlEncodingTestSuite[TestClient]
    with BeforeAndAfterAll {

  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher

  val client: TestClient = new TestClient(
    EndpointsSettings(
      PekkoHttpRequestExecutor
        .cachedHostConnectionPool("localhost", stubServerPortHTTP)
    )
  )

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }
}
