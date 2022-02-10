package authentication

import endpoints4s.{Valid, Validated}
import pdi.jwt.JwtAlgorithm

import java.security.{PrivateKey, PublicKey}

//#enriched-algebra
import endpoints4s.algebra
import endpoints4s.Codec

//#enriched-algebra

//#server-interpreter
import endpoints4s.http4s.server
import pdi.jwt.JwtCirce

//#server-interpreter
//#client-interpreter
import endpoints4s.http4s.client

//#client-interpreter
import endpoints4s.Tupler
import io.circe.{Codec => CirceCodec, Json, HCursor, Decoder}
import org.http4s.headers.Authorization
import org.http4s.{Credentials, AuthScheme}

//#enriched-algebra
/** Algebra interface for defining authenticated endpoints using JWT.
  */
trait Authentication extends algebra.Endpoints with algebra.JsonEntitiesFromSchemas {

  /** Authentication information. It is left abstract because clients and
    * servers may want to use different representations
    */
  type AuthenticationToken

  /** A response containing a JWT in a JSON document. */
  final def authenticationToken: Response[AuthenticationToken] = {
    val authenticationTokenSchema =
      field[String]("jwt_token")
        .xmapWithCodec(authenticationTokenCodec)
    ok(jsonResponse(authenticationTokenSchema))
  }

  /** Logic for decoding the JWT.
    * Servers validate the token signature, clients just decode without validating.
    */
  def authenticationTokenCodec: Codec[String, AuthenticationToken]

  /** A response that might signal to the client that his request was invalid using
    * a `BadRequest` status.
    * Clients map `BadRequest` statuses to `None`, and the underlying `response` into `Some`.
    * Conversely, servers build a `BadRequest` response on `None`, or the underlying `response` otherwise.
    */
  final def wheneverValid[A](responseA: Response[A]): Response[Option[A]] =
    responseA
      .orElse(response(BadRequest, emptyResponse))
      .xmap(_.fold[Option[A]](Some(_), _ => None))(_.toLeft(()))
//#enriched-algebra

  // The following two methods are internally used by interpreters to implement the authentication logic

//#protected-endpoints-algebra
  /** A request with the given `method`, `url` and `entity`, and which is rejected by the server if it
    * doesn’t contain a valid JWT.
    */
  private[authentication] def authenticatedRequest[U, E, UE, UET](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E]
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerUET: Tupler.Aux[UE, AuthenticationToken, UET]
  ): Request[UET]

  /** A response that might signal to the client that his request was not authenticated.
    * Clients throw an exception if the response status is `Unauthorized`.
    * Servers build an `Unauthorized` response in case the incoming request was not correctly authenticated.
    */
  private[authentication] def wheneverAuthenticated[A](
      response: Response[A]
  ): Response[A]

  /** User-facing constructor for endpoints requiring authentication.
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
      authenticatedRequest(method, url, requestEntity),
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

  implicit val codec: CirceCodec[UserInfo] = new CirceCodec[UserInfo] {
    def apply(info: UserInfo): Json = Json.obj("name" -> Json.fromString(info.name))
    def apply(cursor: HCursor): Decoder.Result[UserInfo] =
      cursor.downField("name").as[String].map(UserInfo(_))
  }

}

//#client-interpreter
/** Interpreter for the [[Authentication]] algebra interface that produces
  * an http4s client (using `org.http4s.client.Client`).
  */
trait ClientAuthentication[F[_]]
    extends client.Endpoints[F]
    with client.JsonEntitiesFromSchemas
    with Authentication {

  def publicKey: PublicKey

  // The constructor is private so that users can not
  // forge instances themselves
  class AuthenticationToken private[ClientAuthentication] (
      private[ClientAuthentication] val token: String,
      val decoded: UserInfo
  )

  // Decodes the user info from an OK response
  def authenticationTokenCodec: Codec[String, AuthenticationToken] =
    Codec.fromEncoderAndDecoder[String, AuthenticationToken](_.token) { token =>
      Validated.fromTry(
        JwtCirce
          .decode(token, publicKey)
          .flatMap(claim =>
            io.circe.parser
              .parse(claim.content)
              .toTry
              .flatMap(
                _.as[UserInfo].toTry.map(userInfo => new AuthenticationToken(token, userInfo))
              )
          )
      )
    }

//#client-interpreter
//#protected-endpoints-client
  def authenticatedRequest[U, E, UE, UET](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E]
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerUET: Tupler.Aux[UE, AuthenticationToken, UET]
  ): Request[UET] = {
    // Encodes the user info as a JWT object in the `Authorization` request header
    val authenticationTokenRequestHeaders: RequestHeaders[AuthenticationToken] = {
      (user, http4sRequest) =>
        http4sRequest.putHeaders(
          Authorization(Credentials.Token(AuthScheme.Bearer, user.token))
        )
    }
    request(method, url, entity, headers = authenticationTokenRequestHeaders)
  }

  // Checks that the response is not `Unauthorized` before continuing
  def wheneverAuthenticated[A](response: Response[A]): Response[A] = { (status, headers) =>
    if (status == Unauthorized) {
      Some(_ => effect.raiseError(new Exception("Unauthorized")))
    } else {
      response(status, headers)
    }
  }
//#protected-endpoints-client
//#client-interpreter

}
//#client-interpreter

/** Interpreter for the [[Authentication]] algebra interface that produces
  * an http4s server.
  */
//#server-interpreter
trait ServerAuthentication[F[_]]
    extends server.Endpoints[F]
    with server.JsonEntitiesFromSchemas
    with Authentication {

  def privateKey: PrivateKey
  def publicKey: PublicKey

  // On server side, we build the token ourselves so we only care about the user information
  type AuthenticationToken = UserInfo

  def decodeToken(token: String): Validated[UserInfo] =
    Validated.fromTry(
      JwtCirce
        .decode(token, publicKey)
        .flatMap { claim =>
          io.circe.parser.parse(claim.content).toTry.flatMap(_.as[UserInfo].toTry)
        }
    )

  // Encodes the user info in the JWT session
  def authenticationTokenCodec: Codec[String, AuthenticationToken] =
    Codec.fromEncoderAndDecoder[String, AuthenticationToken] { authenticationToken =>
      JwtCirce.encode(UserInfo.codec(authenticationToken), privateKey, JwtAlgorithm.RS256)
    }(decodeToken(_))
//#server-interpreter

//#protected-endpoints-server
  def authenticatedRequest[U, E, UE, UET](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E]
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerUET: Tupler.Aux[UE, AuthenticationToken, UET]
  ): Request[UET] = {
    // Extracts and validates user info from a request header
    val authenticationTokenRequestHeaders: RequestHeaders[Option[AuthenticationToken]] = {
      headers =>
        {
          Valid(
            headers
              .get[Authorization]
              .flatMap {
                case Authorization(Credentials.Token(AuthScheme.Bearer, token)) =>
                  decodeToken(token).toEither.toOption
                case _ => None
              }
          )
        }
    }

    // alias parameters to not clash with `Request` members
    val urlArg = url
    val methodArg = method
    val entityArg = entity

    new Request[UET] {
      type UrlData = U
      type HeadersData = Option[AuthenticationToken]
      type EntityData = E

      def url: Url[U] = urlArg
      def headers: RequestHeaders[Option[UserInfo]] = authenticationTokenRequestHeaders
      def method: Method = methodArg
      def entity: RequestEntity[E] = entityArg

      def aggregateAndValidate(
          urlData: U,
          headersData: Option[UserInfo],
          entityData: E
      ): Validated[UET] =
        headersData match {
          case None        => sys.error("Unsupported")
          case Some(token) => Valid(tuplerUET(tuplerUE(urlData, entityData), token))
        }

      def matchAndParseHeaders(
          http4sRequest: org.http4s.Request[F]
      ): Option[Either[org.http4s.Response[F], Validated[(UrlData, HeadersData)]]] =
        matchAndParseHeadersAsRight(method, url, headers, http4sRequest).map(_.flatMap {
          case Valid((_, None /* credentials */ )) => Left(org.http4s.Response(Unauthorized))
          case validatedUrlAndHeaders              => Right(validatedUrlAndHeaders)
        })

    }
  }

  // Does nothing because `authenticatedReqest` already
  // takes care of returning `Unauthorized` if the request
  // is not properly authenticated
  def wheneverAuthenticated[A](response: Response[A]): Response[A] = response
//#protected-endpoints-server
//#server-interpreter

}
//#server-interpreter
