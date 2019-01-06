package authentication

//#enriched-algebra
import endpoints.algebra

//#enriched-algebra

//#server-interpreter
import endpoints.play.server
import pdi.jwt.JwtSession
import pdi.jwt.JwtSession.RichResult

//#server-interpreter
//#client-interpreter
import endpoints.play.client

//#client-interpreter
import endpoints.Tupler
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{OFormat, __}
import play.api.libs.functional.syntax._
import play.api.mvc.Results

//#enriched-algebra
/**
  * Algebra interface for defining authenticated endpoints using JWT.
  */
trait Authentication extends algebra.Endpoints {

  /** Authentication information */
  type AuthenticationToken

  /** A response entity containing the authenticated user info
    *
    * Clients decode the JWT attached to the response.
    * Servers encode the authentication information as a JWT and attach it to their response.
    */
  def authenticationToken: Response[AuthenticationToken]

  /** A response that might signal to the client that his request was invalid using
    * a `BadRequest` status.
    * Clients map `BadRequest` statuses to `None`, and the underlying `response` into `Some`.
    * Conversely, servers build a `BadRequest` response on `None`, or the underlying `response` otherwise.
    */
  def wheneverValid[A](response: Response[A]): Response[Option[A]]
//#enriched-algebra

  // The following two methods are internally used by interpreters to implement the authentication logic

//#protected-endpoints-algebra
  /** Defines that the `Authorization` request header contains the authenticated user information,
    * encoded as a serialized JWT object with a `user` field.
    */
  private[authentication] def authenticationTokenRequestHeaders: RequestHeaders[AuthenticationToken]

  /** A response that might signal to the client that his request was not authenticated.
    * Clients throw an exception if the response status is `Unauthorized`.
    * Servers build an `Unauthorized` response in case the incoming request was not correctly authenticated.
    */
  private[authentication] def wheneverAuthenticated[A](response: Response[A]): Response[A]

  /**
    * User-facing constructor for endpoints requiring authentication.
    *
    * @return An endpoint requiring a authentication information to be provided
    *         in the `Authorization` request header. It returns `response`
    *         if the request is correctly authenticated, otherwise it returns
    *         an empty `Unauthorized` response.
    *
    * @param method        HTTP method
    * @param url           Request URL
    * @param response      HTTP response
    * @param requestEntity HTTP request entity
    * @tparam U Information carried by the URL
    * @tparam E Information carried by the request entity
    * @tparam R Information carried by the response
    */
  final def authenticatedEndpoint[U, E, R, UE, UET](
    method: Method,
    url: Url[U],
    requestEntity: RequestEntity[E],
    response: Response[R]
  )(implicit
    tuplerUE: Tupler.Aux[U, E, UE],
    tuplerUET: Tupler.Aux[UE, AuthenticationToken, UET]
  ): Endpoint[UET, R] =
    endpoint(
      request(method, url, requestEntity, authenticationTokenRequestHeaders),
      wheneverAuthenticated(response)
    )
//#protected-endpoints-algebra
//#enriched-algebra

}
//#enriched-algebra

/** Put whatever you need here… */
//#user-info-type
case class UserInfo(name: String)
//#user-info-type

object UserInfo {

  implicit val oformat: OFormat[UserInfo] =
    (__ \ "name").format[String].inmap(UserInfo(_), (_: UserInfo).name)

  def decodeToken(token: String): Option[UserInfo] =
    JwtSession.deserialize(token.trim).getAs[UserInfo]("user")

}

//#client-interpreter
/**
  * Interpreter for the [[Authentication]] algebra interface that produces
  * a Play client (using `play.api.libs.ws.WSClient`).
  */
trait ClientAuthentication
  extends client.Endpoints
    with Authentication {

  // The constructor is private so that users can not
  // forge instances themselves
  class AuthenticationToken private[ClientAuthentication](
    private[ClientAuthentication] val token: String,
    val decoded: UserInfo
  )

  // Decodes the user info from a response
  def authenticationToken: Response[AuthenticationToken] = { httpResponse =>
    httpResponse.headers.get(HeaderNames.AUTHORIZATION) match {
      case Some(Seq(headerValue)) =>
        val token = headerValue.stripPrefix("Bearer ")
        // Note: the default implementation of `JwtSession.deserialize`
        // returns an “empty” JwtSession object when it is invalid.
        // You might want to tweak the logic to return an error in such a case.
        UserInfo.decodeToken(token) match {
          case Some(user) => Right(new AuthenticationToken(token,user))
          case None       => Left(new Exception("Invalid JWT session"))
        }
      case _ => Left(new Exception("Missing JWT session"))
    }
  }

  // Checks that the response is not `BadRequest` before continuing
  def wheneverValid[A](response: Response[A]): Response[Option[A]] = { httpResponse =>
    if (httpResponse.status == Status.BAD_REQUEST) {
      Right(None)
    } else {
      response(httpResponse).right.map(Some(_))
    }
  }
//#client-interpreter
//#protected-endpoints-client
  // Encodes the user info as a JWT object in the `Authorization` request header
  def authenticationTokenRequestHeaders: RequestHeaders[AuthenticationToken] = { (user, wsRequest) =>
    wsRequest.withHttpHeaders(HeaderNames.AUTHORIZATION -> s"Bearer ${user.token}")
  }

  // Checks that the response is not `Unauthorized` before continuing
  def wheneverAuthenticated[A](response: Response[A]): Response[A] = { httpResponse =>
    if (httpResponse.status == Status.UNAUTHORIZED) {
      Left(new Exception("Unauthorized"))
    } else {
      response(httpResponse)
    }
  }
//#protected-endpoints-client
//#client-interpreter

}
//#client-interpreter

/**
  * Interpreter for the [[Authentication]] algebra interface that produces
  * a Play server.
  */
//#server-interpreter
trait ServerAuthentication
  extends Authentication
    with server.Endpoints {

  // On server side, we build the token ourselves so we only care about the user information
  type AuthenticationToken = UserInfo

  // Encodes the user info in the JWT session
  def authenticationToken: Response[UserInfo] =
    userInfo => Results.Ok.withJwtSession(JwtSession().+("user", userInfo))

  // Returns `BadRequest` in case of `None`
  def wheneverValid[A](response: Response[A]): Response[Option[A]] = {
    case None    => Results.BadRequest
    case Some(a) => response(a)
  }
//#server-interpreter

//#protected-endpoints-server
  // Extracts and validates user info from a request header
  def authenticationTokenRequestHeaders: RequestHeaders[AuthenticationToken] = { headers =>
    headers.get(HeaderNames.AUTHORIZATION)
      .flatMap(headerValue => UserInfo.decodeToken(headerValue.stripPrefix("Bearer "))) match {
        case Some(token) => Right(token)
        case None        => Left(Results.Unauthorized)
      }
  }

  // Does nothing because `authenticationTokenRequestHeaders` already
  // takes care of returning `Unauthorized` if the request
  // is not properly authenticated
  def wheneverAuthenticated[A](response: Response[A]): Response[A] = response
//#protected-endpoints-server
//#server-interpreter

}
//#server-interpreter
