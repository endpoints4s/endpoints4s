package endpoints4s.http4s.client

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import endpoints4s.algebra
import org.http4s.Uri
import org.http4s.asynchttpclient.client.AsyncHttpClient
import org.scalatest.BeforeAndAfterAll

class Http4sClientUrlEncodingTest
    extends algebra.client.UrlEncodingTestSuite[TestJsonSchemaClient[IO]]
    with BeforeAndAfterAll {

  val (ahc, shutdown) =
    AsyncHttpClient.allocate[IO]().unsafeRunSync()

  val client = new TestJsonSchemaClient[IO](
    Uri.Authority(
      host = Uri.RegName("localhost"),
      port = Some(8080)
    ),
    Uri.Scheme.http,
    ahc
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

  override def afterAll(): Unit = {
    shutdown.unsafeRunSync()
    super.afterAll()
  }
}
