package endpoints4s.http4s.client

import endpoints4s.algebra

trait Http4sClientUrlEncodingTest[F[_], T <: Endpoints[
  F
] with algebra.client.ClientEndpointsTestApi]
    extends algebra.client.UrlEncodingTestSuite[T] {

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
