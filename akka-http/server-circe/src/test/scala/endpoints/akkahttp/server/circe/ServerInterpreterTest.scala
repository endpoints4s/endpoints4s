package endpoints.akkahttp.server.circe

import endpoints.akkahttp.server.EndpointsTestSuite
import org.scalatest.{Matchers, WordSpecLike}
import endpoints.akkahttp.server.EndpointsTestApi
import endpoints.akkahttp.server.ServerInterpreterBaseTest

class ServerInterpreterTest extends ServerInterpreterBaseTest(new EndpointsCodecsTestApi)
  with EndpointsTestSuite[EndpointsTestApi]
  with WordSpecLike
  with Matchers {

  serverTestSuite()

}
