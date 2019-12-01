package endpoints.akkahttp.server

import org.scalatest.{Matchers, WordSpecLike}

class ServerInterpreterTest extends ServerInterpreterBaseTest(new EndpointsCodecsTestApi)
  with EndpointsTestSuite[EndpointsTestApi]
  with WordSpecLike
  with Matchers {

  serverTestSuite()

}
