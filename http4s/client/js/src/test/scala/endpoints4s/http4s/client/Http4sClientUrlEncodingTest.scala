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
    Uri.Authority(
      host = Uri.RegName("localhost"),
      port = Some(8080)
    ),
    Uri.Scheme.http,
    FetchClientBuilder[IO].create
  )

  def encodeUrl[A](url: client.Url[A])(a: A): String = {
    val (path, query) = url.encodeUrl(a)
    (path.isEmpty, query.isEmpty) match {
      case (true, true)   => ""
      case (false, true)  => s"/${path.renderString}"
      case (true, false)  => s"?${query.renderString}"
      case (false, false) => s"/${path.renderString}?${query.renderString}"
    }
  }
}
