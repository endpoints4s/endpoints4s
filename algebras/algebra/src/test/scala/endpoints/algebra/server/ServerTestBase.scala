package endpoints.algebra.server

import java.net.ServerSocket

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import endpoints.algebra
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

trait ServerTestBase[T <: algebra.Endpoints] extends WordSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll
  with BeforeAndAfter {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds, 10.millisecond)

  val port = findOpenPort

  private def findOpenPort: Int = {
    val socket = new ServerSocket(0)
    try socket.getLocalPort
    finally if (socket != null) socket.close()
  }

  val serverApi: T

  def serveEndpoint[Resp](endpoint: serverApi.Endpoint[_, Resp], response: Resp): Server

}

trait Server {

  def start(): Unit

  def stop(): Unit

}


