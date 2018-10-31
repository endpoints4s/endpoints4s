package endpoints.akkahttp.server

import endpoints.algebra.server.EndpointsTestSuite
import org.scalatest.{Matchers, WordSpecLike}

class ServerInterpreterTest extends ServerInterpreterBaseTest
  with EndpointsTestSuite[EndpointsTestApi]
  with WordSpecLike
  with Matchers {

  serverTestSuite()

}
