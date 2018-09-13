package endpoints.algebra.client

import java.net.ServerSocket

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import endpoints.algebra
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.Future
import scala.concurrent.duration._

trait ClientTestBase[T <: algebra.Endpoints] extends WordSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll
  with BeforeAndAfter {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(15.seconds, 10.millisecond)

  val wiremockPort = findOpenPort
  val wireMockServer = new WireMockServer(options().port(wiremockPort))

  override def beforeAll(): Unit = wireMockServer.start()

  override def afterAll(): Unit = wireMockServer.stop()

  before {
    wireMockServer.resetAll()
  }

  private def findOpenPort: Int = {
    val socket = new ServerSocket(0)
    try socket.getLocalPort
    finally if (socket != null) socket.close()
  }

  val client: T

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp]

}
