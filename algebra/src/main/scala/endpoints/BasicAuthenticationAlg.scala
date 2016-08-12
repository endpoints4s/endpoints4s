package endpoints

trait BasicAuthenticationAlg extends EndpointAlg {

  def basicAuthentication: Headers[Credentials]

}

case class Credentials(username: String, password: String)
