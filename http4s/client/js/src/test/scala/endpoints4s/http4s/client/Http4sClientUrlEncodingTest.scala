package endpoints4s.http4s.client

import cats.effect.IO
import endpoints4s.algebra
import org.http4s.Uri
import org.http4s.dom.FetchClientBuilder
import org.scalatest.BeforeAndAfterAll

class Http4sClientUrlEncodingTest
    extends algebra.client.UrlEncodingTestSuite[TestJsonSchemaClient[IO]]
    with BeforeAndAfterAll {

  val client = new TestJsonSchemaClient[IO](
    Uri.unsafeFromString(s"http://localhost:8080"),
    FetchClientBuilder[IO].create
  )

  def encodeUrl[A](url: client.Url[A])(a: A): String =
    url.encodeUrl(a).toOption.get.renderString
}
