package endpoints.akkahttp.server

import org.scalatest.{Matchers, WordSpecLike}

class ServerInterpreterTest extends ServerInterpreterBaseTest
  with EndpointsTestSuite[EndpointsTestApi]
  with WordSpecLike
  with Matchers {

  serverTestSuite()

}
