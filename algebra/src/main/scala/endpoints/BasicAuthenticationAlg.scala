package endpoints

trait BasicAuthenticationAlg extends EndpointsAlg {

  def authenticatedGet[A](url: Url[A])(implicit tupler: Tupler[A, Credentials]): Request[tupler.Out]

}

case class Credentials(username: String, password: String)
