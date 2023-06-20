package endpoints4s.http4s.server


import endpoints4s.algebra.server.{
  AssetsTestSuite,
  BasicAuthenticationTestSuite,
  EndpointsTestSuite,
  JsonEntitiesFromSchemasTestSuite,
  SumTypedEntitiesTestSuite,
  TextEntitiesTestSuite
}

class ServerInterpreterTest
    extends Http4sServerTest[EndpointsTestApi]
    with EndpointsTestSuite[EndpointsTestApi]
    with BasicAuthenticationTestSuite[EndpointsTestApi]
    with JsonEntitiesFromSchemasTestSuite[EndpointsTestApi]
    with TextEntitiesTestSuite[EndpointsTestApi]
    with SumTypedEntitiesTestSuite[EndpointsTestApi]
    with AssetsTestSuite[EndpointsTestApi]
    with AssetsResourcesTest {

  val serverApi = new EndpointsTestApi

  def assetsResources(pathPrefix: Option[String]) =
    serverApi.assetsResources(pathPrefix)

  def serveAssetsEndpoint(
      endpoint: serverApi.Endpoint[
        serverApi.AssetRequest,
        serverApi.AssetResponse
      ],
      response: => serverApi.AssetResponse
  )(runTests: Int => Unit): Unit =
    serveEndpoint(endpoint, response)(runTests)

}
