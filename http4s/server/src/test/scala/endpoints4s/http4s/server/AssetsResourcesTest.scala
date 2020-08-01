package endpoints4s.http4s.server

import endpoints4s.algebra.server.ServerTestBase
import org.http4s.HttpDate

trait AssetsResourcesTest {
  self: ServerTestBase[EndpointsTestApi] =>

  def assetsResources(pathPrefix: Option[String]): serverApi.AssetRequest => serverApi.AssetResponse
  def request2response = assetsResources(Some("/assets"))

  "assetsResources" should {

    "respond Found for existing asset" in {
      val request = serverApi.AssetRequest(
        serverApi.AssetPath(Seq(), "asset1.txt"),
        false,
        None
      )

      val response = request2response(request)
      response shouldBe a[serverApi.AssetResponse.Found]
    }

    "respond NotFound for non-existing asset" in {
      val request = serverApi.AssetRequest(
        serverApi.AssetPath(Seq(), "asset-non-existing.txt"),
        false,
        None
      )

      val response = request2response(request)
      response should be theSameInstanceAs serverApi.AssetResponse.NotFound
    }

    "evaluate If-Modified-Since header (rfc7232)" in {
      val request = serverApi.AssetRequest(
        serverApi.AssetPath(Seq(), "asset1.txt"),
        false,
        Some(HttpDate.MaxValue)
      )

      val response = request2response(request)

      response should have(
        Symbol("isExpired")(false)
      )
    }
  }

}
