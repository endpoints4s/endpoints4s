package endpoints.akkahttp.server

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ServerInterpreterTest extends ServerInterpreterBaseTest(new EndpointsCodecsTestApi)
  with EndpointsTestSuite[EndpointsTestApi]
  with AnyWordSpecLike
  with Matchers {

  serverTestSuite()

}
