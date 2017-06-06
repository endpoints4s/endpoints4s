package endpoints.testsuite.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpec}

import endpoints.algebra

trait ClientTestBase[T <: algebra.Endpoints] extends WordSpec
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfter {

  val wiremockPort = 19211

  val wireMockServer = new WireMockServer(options().port(wiremockPort))

  override def beforeAll(): Unit = wireMockServer.start()

  override def afterAll(): Unit = wireMockServer.stop()

  before {
    wireMockServer.resetAll()
  }


  val client: T

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Resp

}
