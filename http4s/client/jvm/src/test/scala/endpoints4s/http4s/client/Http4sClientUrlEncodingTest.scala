package endpoints4s.http4s.client

import cats.effect.IO
import endpoints4s.algebra
import org.http4s.Uri
import org.http4s.asynchttpclient.client.AsyncHttpClient
import org.scalatest.BeforeAndAfterAll

import cats.effect.unsafe.implicits.global

class Http4sClientUrlEncodingTest
    extends algebra.client.UrlEncodingTestSuite[TestJsonSchemaClient[IO]]
    with BeforeAndAfterAll {

  val (ahc, shutdown) =
    AsyncHttpClient.allocate[IO]().unsafeRunSync()

  val client = new TestJsonSchemaClient[IO](
    Uri.unsafeFromString(s"http://localhost:8080"),
    ahc
  )

  def encodeUrl[A](url: client.Url[A])(a: A): String =
    url.encodeUrl(a).toOption.get.renderString

  override def afterAll(): Unit = {
    shutdown.unsafeRunSync()
    super.afterAll()
  }
}
