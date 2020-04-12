package endpoints.algebra

import scala.util.{Failure, Success, Try}

trait EndpointsDocs extends Endpoints {

  locally {
    //#construction
    // An endpoint whose requests use the HTTP verb “GET” and the URL
    // path “/some-resource”, and whose responses have an entity of
    // type “text/plain”
    val someResource: Endpoint[Unit, String] =
      endpoint(get(path / "some-resource"), ok(textResponse))
    //#construction
  }

  //#with-docs
  endpoint(
    get(path / "some-resource"),
    ok(textResponse),
    docs = EndpointDocs(description = Some("The contents of some resource"))
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
  import endpoints.{Invalid, Valid}

  implicit def localDateSegment(
      implicit string: Segment[String]
  ): Segment[LocalDate] =
    string.xmapPartial { s =>
      Try(LocalDate.parse(s)) match {
        case Failure(_)    => Invalid(s"Invalid date value '$s'")
        case Success(date) => Valid(date)
      }
    }(_.toString)
  //#xmap-partial
}

//#documented-endpoint-definition
import endpoints.algebra

trait DocumentedEndpoints extends algebra.Endpoints {

  val someDocumentedResource: Endpoint[Int, String] =
    endpoint(
      get(path / "some-resource" / segment[Int]("id")),
      ok(textResponse, docs = Some("The content of the resource"))
    )

}
//#documented-endpoint-definition
