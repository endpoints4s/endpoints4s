package authentication

//#login-endpoint
import endpoints4s.algebra

//#login-endpoint
//#login-implementation
import endpoints4s.http4s.server

//#login-implementation
import java.security.KeyPairGenerator
import endpoints4s.http4s.client
import org.http4s.client.{Client => Http4sClient}
import org.http4s.Uri
import cats.effect.IO

/** Example of endpoints making use of authentication */
//#login-endpoint
trait AuthenticationEndpoints extends algebra.Endpoints with Authentication {

  /** Login endpoint: takes the API key in a query string parameter and returns either `Some(authenticationToken)`
    * if the credentials are valid, or `None` otherwise
    */
  val login: Endpoint[String, Option[AuthenticationToken]] = endpoint(
    get(path / "login" /? qs[String]("apiKey")),
    wheneverValid(authenticationToken)
  )
//#login-endpoint

//#protected-endpoint
  /** Some resource requiring the request to provide a valid JWT token. Returns a message
    * “Hello ''user_name''” if the request is correctly authenticated, otherwise returns
    * an `Unauthorized` HTTP response.
    */
  val someResource: Endpoint[AuthenticationToken, String] =
    authenticatedEndpoint(
      Get,
      path / "some-resource",
      emptyRequest,
      ok(textResponse)
    )
//#protected-endpoint
//#login-endpoint

}
//#login-endpoint

// (In practice these would be provided via environment variables)
object Keys {
  val generator = KeyPairGenerator.getInstance("RSA")
  generator.initialize(2048)
  val pair = generator.generateKeyPair()
}

/** Client for the `AuthenticationEndpoints`, using the `ClientAuthentication`
  * interpreter (implementing the authentication logic), defined below.
  */
class Client(
    authority: Uri.Authority,
    scheme: Uri.Scheme,
    http4sClient: Http4sClient[IO]
) extends client.Endpoints[IO](authority, scheme, http4sClient)
    with AuthenticationEndpoints
    with ClientAuthentication[IO] {

  lazy val publicKey = Keys.pair.getPublic

}

/** Example of server implementing the `AuthenticationEndpoints`
  */
//#login-implementation
class Server
    extends server.Endpoints[IO]
    with AuthenticationEndpoints
    with ServerAuthentication[IO] {

//#login-implementation
  // In practice, this secret would be ... secret, it would be retrieved from some configuration
  lazy val privateKey = Keys.pair.getPrivate
  lazy val publicKey = Keys.pair.getPublic

  val routes = routesFromEndpoints(
//#login-implementation
    login.implementedBy { apiKey =>
      if (apiKey == "foobar") Some(UserInfo("Alice"))
      else None
    }
//#login-implementation
    ,
//#protected-resource-implementation
    // Note that the `AuthenticationToken` is available to the implementations
    // It can be used to check authorizations
    someResource.implementedBy(token => s"Hello ${token.name}!")
//#protected-resource-implementation
  )
//#login-implementation

}
//#login-implementation
