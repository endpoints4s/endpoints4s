package endpoints.akkahttp.server.circe

import endpoints.akkahttp.server.EndpointsTestSuite
import org.scalatest.{Matchers, WordSpecLike}

class ServerInterpreterTest extends ServerInterpreterBaseTest
  with EndpointsTestSuite[EndpointsTestApi]
  with WordSpecLike
  with Matchers {

  serverTestSuite()

}
