package endpoints4s.algebra

import scala.util.{Failure, Success, Try}
import scala.annotation.nowarn

@nowarn("cat=other-pure-statement")
trait EndpointsDocs extends Endpoints {

  locally {
    //#construction
    // An endpoint whose requests use the HTTP verb “GET” and the URL
    // path “/some-resource”, and whose responses have an entity of
    // type “text/plain”
    val someResource: Endpoint[Unit, String] =
      endpoint(get(path / "some-resource"), ok(textResponse))
    //#construction
  }: @nowarn("cat=unused-locals")

  //#with-docs
  endpoint(
    get(path / "some-resource"),
    ok(textResponse),
    docs = EndpointDocs().withDescription(Some("The contents of some resource"))
  )
  //#with-docs

  //#request-construction
  // A request that uses the verb “GET”, the URL path “/foo”,
  // no entity, no documentation, and no headers
  request(Get, path / "foo", emptyRequest, None, emptyRequestHeaders)
  //#request-construction

  //#convenient-get
  get(path / "foo") // Same as above
  //#convenient-get

  //#urls
  // the root path: “/”
  path
  // static segment: “/users”
  path / "users"
  // path with trailing slash: “/users/”
  path / "users" / ""
  // path parameter: “/users/1234”, “/users/5678”, …
  path / "users" / segment[Long]()
  // path parameter: “/assets/images/logo.png”
  path / "assets" / remainingSegments()
  // query parameter: “/articles?page=2”, “/articles?page=5”, …
  path / "articles" /? qs[Int]("page")
  // optional parameter: “/articles”, “/articles?page=2”, …
  path / "articles" /? qs[Option[Int]]("page")
  // repeated parameter: “/articles?kinds=garden&kinds=woodworking”, …
  path / "articles" /? qs[List[String]]("kinds")
  // several parameters: “/?q=foo&lang=en”, …
  path /? (qs[String]("q") & qs[String]("lang"))
  //#urls

  //#urls-with-docs
  // “/users/{id}”
  path / "users" / segment[Long]("id", docs = Some("A user id"))

  // “/?q=foo&lang=en”, …
  val query = qs[String]("q", docs = Some("Query"))
  val lang = qs[String]("lang", docs = Some("Language"))
  path /? (query & lang)
  //#urls-with-docs

  //#response
  // An HTTP response with status code 200 (Ok) and no entity
  val nothing: Response[Unit] = ok(emptyResponse)
  //#response

  //#general-response
  // An HTTP response with status code 200 (Ok) and a text entity
  val aTextResponse: Response[String] = response(OK, textResponse)
  //#general-response

  //#documented-response
  ok(
    emptyResponse,
    docs = Some("A response with an OK status code and no entity")
  )
  //#documented-response

  // Shared definition used by the documentation of interpreters
  //#endpoint-definition
  val someResource: Endpoint[Int, String] =
    endpoint(get(path / "some-resource" / segment[Int]()), ok(textResponse))
  //#endpoint-definition

  //#xmap-partial
  import java.time.LocalDate
  import endpoints4s.{Invalid, Valid}

  implicit def localDateSegment(implicit
      string: Segment[String]
  ): Segment[LocalDate] =
    string.xmapPartial { s =>
      Try(LocalDate.parse(s)) match {
        case Failure(_)    => Invalid(s"Invalid date value '$s'")
        case Success(date) => Valid(date)
      }
    }(_.toString)
  //#xmap-partial

  locally {
    //#without-middlewares
    val xApiKeyHeader = requestHeader("X-API-Key", Some("API Key"))

    val foo = endpoint(
      get(path / "foo", headers = xApiKeyHeader),
      ok(textResponse)
    )

    val bar = endpoint(
      get(path / "bar", headers = xApiKeyHeader),
      ok(textResponse)
    )
    //#without-middlewares
  }: @nowarn("cat=unused-locals")

  locally {
    //#with-middlewares
    def withApiKey[A](request: Request[A]): Request[(A, String)] =
      request.addHeaders(requestHeader("X-API-Key", Some("API Key")))

    val foo = endpoint(
      withApiKey(get(path / "foo")),
      ok(textResponse)
    )

    val bar = endpoint(
      withApiKey(get(path / "bar")),
      ok(textResponse)
    )
    //#with-middlewares
  }: @nowarn("cat=unused-locals")

  locally {
    //#api-key-query
    def withApiKey[A](request: Request[A]): Request[(A, String)] =
      request.addQueryString(qs[String]("api_key"))
    //#api-key-query
  }: @nowarn("cat=unused-locals")

  //#with-authentication-definition
  def withAuthentication[A, B](
      endpoint: Endpoint[A, B]
  ): Endpoint[(A, String), Either[String, B]] =
    endpoint
      .mapRequest { (endpointRequest: Request[A]) =>
        endpointRequest.addHeaders(requestHeader("Authorization"))
      }
      .mapResponse { (endpointResponse: Response[B]) =>
        val unauthorizedResponse: Response[String] =
          response(
            Unauthorized,
            emptyResponse,
            headers = responseHeader("WWW-Authenticate")
          )
        unauthorizedResponse.orElse(endpointResponse)
      }
  //#with-authentication-definition

  //#with-authentication-usage
  val unauthenticatedEndpoint: Endpoint[Int, String] =
    endpoint(
      get(path / "foo" /? qs[Int]("n")),
      ok(textResponse)
    )
  val authenticatedEndpoint: Endpoint[(Int, String), Either[String, String]] =
    withAuthentication(unauthenticatedEndpoint)
  //#with-authentication-usage

}

//#documented-endpoint-definition
import endpoints4s.algebra

trait DocumentedEndpoints extends algebra.Endpoints {

  val someDocumentedResource: Endpoint[Int, String] =
    endpoint(
      get(path / "some-resource" / segment[Int]("id")),
      ok(textResponse, docs = Some("The content of the resource"))
    )

}
//#documented-endpoint-definition
