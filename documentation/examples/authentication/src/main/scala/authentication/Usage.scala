package authentication

//#login-endpoint
import endpoints.algebra

//#login-endpoint
//#login-implementation
import endpoints.play.server

//#login-implementation
import endpoints.play.client
import play.api.libs.ws.WSClient
import play.api.BuiltInComponents

import scala.concurrent.ExecutionContext

/** Example of endpoints making use of authentication */
//#login-endpoint
trait AuthenticationEndpoints
  extends algebra.Endpoints
    with Authentication {

  /**
    * Login endpoint: takes the API key in a query string parameter and returns either `Some(authenticationToken)`
    * if the credentials are valid, or `None` otherwise
    */
  val login = endpoint(
    get(path / "login" /? qs[String]("apiKey")),
    wheneverValid(authenticationToken)
  )
//#login-endpoint

//#protected-endpoint
  /**
    * Some resource requiring the request to provide a valid JWT token. Returns a message
    * “Hello ''user_name''” if the request is correctly authenticated, otherwise returns
    * an `Unauthorized` HTTP response.
    */
  val someResource: Endpoint[AuthenticationToken, String] = authenticatedEndpoint(
    Get,
    path / "some-resource",
    emptyRequest,
    textResponse()
  )
//#protected-endpoint
//#login-endpoint

}
//#login-endpoint

/**
  * Client for the `AuthenticationEndpoints`, using the `ClientAuthentication`
  * interpreter (implementing the authentication logic), defined below.
  */
class Client(host: String, wsClient: WSClient)(implicit ec: ExecutionContext)
  extends client.Endpoints(host, wsClient)
    with AuthenticationEndpoints
    with ClientAuthentication

/**
  * Example of server implementing the `AuthenticationEndpoints`
  */
//#login-implementation
class Server(val playComponents: BuiltInComponents)
  extends AuthenticationEndpoints
    with server.Endpoints
    with ServerAuthentication {

  //#login-implementation

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
