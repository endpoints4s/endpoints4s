package endpoints4s.scalaj.client

import endpoints4s.algebra

class ScalajEndpointsUrlEncodingTest extends algebra.client.UrlEncodingTestSuite[TestClient] {

  val client: TestClient = new TestClient(s"localhost:8080")

  def encodeUrl[A](url: client.Url[A])(a: A): String = {
    val req = url.toReq(a)
    val encodedUrl = req.urlBuilder(req)
    val pathAndQuery =
      encodedUrl.drop(
        s"http://localhost:8080".size
      ) // Remove scheme, host and port from URL
    if (pathAndQuery.startsWith("/?") || pathAndQuery == "/")
      pathAndQuery.drop(
        1
      ) // For some reason, scalaj always inserts a slash when the path is empty
    else pathAndQuery
  }
}
