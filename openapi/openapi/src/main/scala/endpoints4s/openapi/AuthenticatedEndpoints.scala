package endpoints4s
package openapi

import endpoints4s.openapi.model.{SecurityRequirement, SecurityScheme}

/** Interpreter for [[endpoints4s.algebra.AuthenticatedEndpoints]] that produces
  * OpenAPI documentation.
  *
  * @group interpreters
  */
trait AuthenticatedEndpoints extends algebra.AuthenticatedEndpoints with EndpointsWithCustomErrors {

  def basicAuthSchemeName: String = "HttpBasic"
  def bearerAuthSchemeName: String = "Bearer"
  def bearerAuthBearerFormat: String = "JWT"

  type UsernamePassword = String
  lazy val basicAuthHeader: RequestHeaders[UsernamePassword] =
    requestHeader("Authorization")

  type BearerToken = String
  lazy val bearerAuthHeader: RequestHeaders[BearerToken] =
    requestHeader("Authorization")

  override def endpointWithBasicAuth[A, B](
      endpoint: Endpoint[A, B],
      realm: Option[String]
  )(implicit tupler: Tupler[A, UsernamePassword]): Endpoint[tupler.Out, Option[B]] =
    super
      .endpointWithBasicAuth(endpoint, realm)
      .withSecurity(
        SecurityRequirement(
          basicAuthSchemeName,
          SecurityScheme.httpBasic
        )
      )

  override def endpointWithBearerAuth[A, B](
      endpoint: Endpoint[A, B],
      bearerFormat: Option[String] = None
  )(implicit tupler: Tupler[A, BearerToken]): Endpoint[tupler.Out, Option[B]] =
    super
      .endpointWithBearerAuth(endpoint)
      .withSecurity(
        SecurityRequirement(
          bearerAuthSchemeName,
          SecurityScheme.bearer(bearerFormat)
        )
      )
}
