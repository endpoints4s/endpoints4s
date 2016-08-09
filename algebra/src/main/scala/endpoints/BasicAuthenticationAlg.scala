package endpoints

trait BasicAuthenticationAlg extends EndpointsAlg {

  def basicAuthentication: Headers[Credentials]

}

case class Credentials(username: String, password: String)
