package endpoints.akkahttp.server.playjson

import endpoints.akkahttp.server.EndpointsTestSuite
import org.scalatest.{Matchers, WordSpecLike}
import endpoints.akkahttp.server.EndpointsTestApi 

class ServerInterpreterTest extends ServerInterpreterBaseTest
  with EndpointsTestSuite[EndpointsTestApi]
  with WordSpecLike
  with Matchers {

  serverTestSuite()

}
