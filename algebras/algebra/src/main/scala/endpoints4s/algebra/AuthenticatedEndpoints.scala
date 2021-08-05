package endpoints4s.algebra

import endpoints4s.{Codec, Invalid, Tupler, Valid, Validated}

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import scala.util.Try

/** Provides vocabulary to describe endpoints that use Basic HTTP or Bearer
  * token authentication.
  *
  * @group algebras
  */
trait AuthenticatedEndpoints extends Endpoints {

  private[algebra] lazy val base64StringCodec: Codec[String, String] =
    Codec.fromEncoderAndDecoder[String, String](s =>
      Base64.getEncoder().encodeToString(s.getBytes(UTF_8))
    )(s => Validated.fromTry { Try(new String(Base64.getDecoder().decode(s), UTF_8)) })

  lazy val basicAuthCodec: Codec[String, (String, String)] = {
    val basicPrefix = "Basic "

    Codec.fromEncoderAndDecoder[String, (String, String)] { case (username, password) =>
      s"$basicPrefix${base64StringCodec.encode(s"$username:$password")}"
    } { auth =>
      val (prefix, suffix) = auth.splitAt(basicPrefix.length)
      if (prefix == basicPrefix) {
        base64StringCodec.decode(suffix).flatMap { userPass =>
          userPass.indexOf(':') match {
            case -1    => Valid(userPass -> "")
            case index => Valid(userPass.substring(0, index) -> userPass.substring(index + 1))
          }
        }
      } else {
        Invalid("Malformed Authorization header")
      }
    }
  }

  lazy val bearerAuthCodec: Codec[String, String] = {
    val bearerPrefix = "Bearer "

    Codec.fromEncoderAndDecoder[String, String] { token =>
      s"$bearerPrefix${base64StringCodec.encode(token)}"
    } { auth =>
      val (prefix, suffix) = auth.splitAt(bearerPrefix.length)
      if (prefix == bearerPrefix) {
        base64StringCodec.decode(suffix)
      } else {
        Invalid("Malformed Authorization header")
      }
    }
  }

  type UsernamePassword

  /** The `"Authorization"` header for an Basic HTTP authentication. */
  def basicAuthHeader: RequestHeaders[UsernamePassword]

  type BearerToken

  /** The `"Authorization"` header for an Bearer token authentication. */
  def bearerAuthHeader: RequestHeaders[BearerToken]

  def unauthorizedResponse(realm: Option[String]): Response[Unit] =
    response(
      Unauthorized,
      emptyResponse,
      Some("User is not authorized to call that endpoint"),
      optResponseHeader("WWW-Authenticate").xmap(_ => ())(_ =>
        Some(s"""Basic realm="${realm.getOrElse("Realm")}", charset="UTF-8"""")
      )
    )

  /** Add Basic HTTP authentication to an endpoint.
    *
    * @param realm the `"realm"` attribute of the `"WWW-Authenticate"` response header. Defaults to `"Realm"`.
    */
  def endpointWithBasicAuth[A, B](
      endpoint: Endpoint[A, B],
      realm: Option[String] = None
  )(implicit tupler: Tupler[A, UsernamePassword]): Endpoint[tupler.Out, Option[B]] =
    endpoint
      .mapRequest(_.addHeaders(basicAuthHeader))
      .mapResponse(_.orElse(unauthorizedResponse(realm)).xmap(_.left.toOption)(_.toLeft(())))

  /** Add Bearer token authentication to an endpoint.
    *
    * @param bearerFormat the format of the bearer token. Used in OpenApi.
    */
  def endpointWithBearerAuth[A, B](
      endpoint: Endpoint[A, B],
      bearerFormat: Option[String] = None
  )(implicit tupler: Tupler[A, BearerToken]): Endpoint[tupler.Out, Option[B]] =
    endpoint
      .mapRequest(_.addHeaders(bearerAuthHeader))
      .mapResponse(_.orElse(unauthorizedResponse(None)).xmap(_.left.toOption)(_.toLeft(())))

  implicit class AuthenticationEndpointOps[A, B](endpoint: Endpoint[A, B]) {

    /** Add Basic HTTP authentication to this endpoint.
      *
      * @param realm the `"realm"` attribute of the `"WWW-Authenticate"` response header. Defaults to `"Realm"`.
      */
    def withBasicAuth(
        realm: Option[String] = None
    )(implicit tupler: Tupler[A, UsernamePassword]): Endpoint[tupler.Out, Option[B]] =
      endpointWithBasicAuth(endpoint, realm)

    /** Add Bearer token authentication to this endpoint.
      *
      * @param bearerFormat the format of the bearer token. Used in OpenApi.
      */
    def withBearerAuth(
        bearerFormat: Option[String] = None
    )(implicit tupler: Tupler[A, BearerToken]): Endpoint[tupler.Out, Option[B]] =
      endpointWithBearerAuth(endpoint)
  }
}
object AuthenticatedEndpoints {
  case class Credentials(username: String, password: String)
  case class Bearer(token: String)
}

/** Interpreter for [[AuthenticatedEndpoints]] on the client-side.
  *
  * @group interpreters
  */
trait AuthenticatedEndpointsClient extends AuthenticatedEndpoints {

  type UsernamePassword = AuthenticatedEndpoints.Credentials
  lazy val basicAuthHeader: RequestHeaders[UsernamePassword] =
    requestHeader("Authorization")
      .xmapWithCodec(basicAuthCodec)
      .xmap(AuthenticatedEndpoints.Credentials.tupled)(credentials =>
        credentials.username -> credentials.password
      )

  type BearerToken = AuthenticatedEndpoints.Bearer
  lazy val bearerAuthHeader: RequestHeaders[BearerToken] =
    requestHeader("Authorization")
      .xmapWithCodec(bearerAuthCodec)
      .xmap(AuthenticatedEndpoints.Bearer)(_.token)
}

/** Interpreter for [[AuthenticatedEndpoints]] on the server-side.
  *
  * @group interpreters
  */
trait AuthenticatedEndpointsServer extends AuthenticatedEndpoints {

  type UsernamePassword = Option[AuthenticatedEndpoints.Credentials]
  lazy val basicAuthHeader: RequestHeaders[UsernamePassword] =
    optRequestHeader("Authorization")
      .xmapPartial {
        case Some(auth) =>
          basicAuthCodec.decode(auth).map { case (username, password) =>
            Some(AuthenticatedEndpoints.Credentials(username, password))
          }
        case None => Valid(None)
      } {
        _.map(credentials => basicAuthCodec.encode(credentials.username -> credentials.password))
      }

  type BearerToken = Option[AuthenticatedEndpoints.Bearer]
  lazy val bearerAuthHeader: RequestHeaders[BearerToken] =
    optRequestHeader("Authorization")
      .xmapPartial {
        case Some(auth) =>
          bearerAuthCodec.decode(auth).map(token => Some(AuthenticatedEndpoints.Bearer(token)))
        case None => Valid(None)
      } {
        _.map(bearer => bearerAuthCodec.encode(bearer.token))
      }
}
