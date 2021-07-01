package endpoints4s.algebra

import endpoints4s.{Codec, Invalid, Tupler, Valid, Validated}
import endpoints4s.algebra.BasicAuthentication.Credentials
import java.util.Base64
import java.nio.charset.StandardCharsets
import scala.util.Try

/** Provides vocabulary to describe endpoints that use Basic HTTP authentication.
  *
  * This trait works fine, but developers are likely to implement their own
  * authentication mechanism, specific to their application.
  *
  * @group algebras
  */
trait BasicAuthentication extends EndpointsWithCustomErrors {

  /** A response that can either be Forbidden (403) or the given `Response[A]`.
    *
    * The returned `Response[Option[A]]` signals “forbidden” with a `None` value.
    *
    * @param responseA Inner response (in case the authentication succeeds)
    * @param docs Description of the authentication error
    */
  private[endpoints4s] final def authenticated[A](
      responseA: Response[A],
      docs: Documentation = None
  ): Response[
    Option[A]
  ] = // FIXME Use an extensible type to model authentication failure
    responseA
      .orElse(response(Forbidden, emptyResponse, docs))
      .xmap(_.fold[Option[A]](Some(_), _ => None))(_.toLeft(()))

  /** A request with the given `method`, `url`, `entity` and `headers`, but
    * which also contains the Basic Authentication credentials in its
    * “Authorization” header.
    *
    * The `Out` type aggregates together the URL information `U`, the entity
    * information `E`, the headers information `H`, and the `Credentials`.
    *
    * In case the authentication credentials are missing from the request,
    * servers reject the request with an Unauthorized (401) status code.
    */
  private[endpoints4s] def authenticatedRequest[U, E, H, UE, HCred, Out](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E],
      headers: RequestHeaders[H],
      requestDocs: Documentation
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCred: Tupler.Aux[H, Credentials, HCred],
      tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Request[Out]

  /** Describes an endpoint protected by Basic HTTP authentication
    * @group operations
    */
  def authenticatedEndpoint[U, E, R, H, UE, HCred, Out](
      method: Method,
      url: Url[U],
      response: Response[R],
      requestEntity: RequestEntity[E] = emptyRequest,
      requestHeaders: RequestHeaders[H] = emptyRequestHeaders,
      unauthenticatedDocs: Documentation = None,
      requestDocs: Documentation = None,
      endpointDocs: EndpointDocs = EndpointDocs()
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCred: Tupler.Aux[H, Credentials, HCred],
      tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Endpoint[Out, Option[R]] =
    endpoint(
      authenticatedRequest(
        method,
        url,
        requestEntity,
        requestHeaders,
        requestDocs
      ),
      authenticated(response, unauthenticatedDocs),
      endpointDocs
    )

}

object BasicAuthentication {
  case class Credentials(username: String, password: String)
  case class Bearer(token: String)
}

trait AuthenticationMiddlewares extends Middlewares {
  import BasicAuthentication._

  lazy val base64StringCodec: Codec[String, String] =
    Codec.fromEncoderAndDecoder[String, String](s =>
      Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8))
    )(s =>
      Validated.fromTry { Try(new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8)) }
    )

  lazy val basicAuthHeader: RequestHeaders[Option[Credentials]] = {
    val basicPrefix = "Basic "

    optRequestHeader("Authorization").xmapPartial {
      case Some(auth) =>
        val (prefix, suffix) = auth.splitAt(basicPrefix.length)
        if (prefix == basicPrefix) {
          base64StringCodec.decode(suffix).flatMap { userPass =>
            userPass.indexOf(':') match {
              case -1 => Valid(Some(Credentials(userPass, "")))
              case index =>
                Valid(
                  Some(Credentials(userPass.substring(0, index), userPass.substring(index + 1)))
                )
            }
          }
        } else {
          Invalid("Malformed Authorization header")
        }
      case None => Valid(None)
    }(_.map { case Credentials(username, password) =>
      s"Basic ${base64StringCodec.encode(s"$username:$password")}"
    })
  }

  lazy val bearerAuthHeader: RequestHeaders[Bearer] = {
    val bearerPrefix = "Bearer "

    requestHeader("Authorization").xmapPartial { auth =>
      val (prefix, suffix) = auth.splitAt(bearerPrefix.length)
      if (prefix == bearerPrefix) {
        base64StringCodec.decode(suffix).map(Bearer)
      } else {
        Invalid("Malformed Authorization header")
      }
    } { case Bearer(token) =>
      s"Bearer ${base64StringCodec.encode(token)}"
    }
  }

  def wwwAuthHeader(realm: Option[String]): ResponseHeaders[Unit] =
    realm match {
      case Some(realm) =>
        responseHeader("WWW-Authenticate").xmap(_ => ())(_ =>
          s"""Basic real="$realm", charset="UTF-8""""
        )
      case None => emptyResponseHeaders
    }

  def unauthorizedResponse(realm: Option[String]) =
    response(
      Unauthorized,
      emptyResponse,
      Some("User is not authorized to call that endpoint"),
      wwwAuthHeader(realm)
    )

  implicit class AuthenticationEndpointOps[A, B](endpoint: Endpoint[A, B]) {

    def withBasicAuth(realm: Option[String]): Endpoint[(A, Option[Credentials]), Option[B]] =
      endpoint
        .mapRequest(_.withHeaders(basicAuthHeader))
        .mapResponse(_.orElse(unauthorizedResponse(realm)).xmap(_.left.toOption)(_.toLeft(())))

    def withBearerAuth: Endpoint[(A, Bearer), Option[B]] =
      endpoint
        .mapRequest(_.withHeaders(bearerAuthHeader))
        .mapResponse(_.orElse(unauthorizedResponse(None)).xmap(_.left.toOption)(_.toLeft(())))
  }
}
