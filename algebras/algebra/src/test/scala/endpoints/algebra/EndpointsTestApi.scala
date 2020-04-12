package endpoints.algebra

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.UUID

import endpoints.{Invalid, Valid, Validated, algebra}

import scala.util.Try

trait EndpointsTestApi extends algebra.Endpoints {

  val UUIDEndpoint = endpoint(
    get(
      path / "user" / segment[UUID]() / "description" /? (qs[String]("name") & qs[
        Int
      ]("age"))
    ),
    ok(textResponse)
  )

  val putUUIDEndpoint = endpoint(
    put(path / "user" / segment[UUID](), emptyRequest),
    ok(emptyResponse)
  )

  val deleteUUIDEndpoint = endpoint(
    delete(path / "user" / segment[UUID]()),
    ok(emptyResponse)
  )

  val emptyResponseUUIDEndpoint = endpoint(
    get(
      path / "user" / segment[UUID]() / "description" /? (qs[String]("name") & qs[
        Int
      ]("age"))
    ),
    ok(emptyResponse)
  )

  val smokeEndpoint = endpoint(
    get(
      path / "user" / segment[String]() / "description" /? (qs[String]("name") & qs[
        Int
      ]("age"))
    ),
    ok(textResponse)
  )

  val putEndpoint = endpoint(
    put(path / "user" / segment[String](), emptyRequest),
    ok(emptyResponse)
  )

  val deleteEndpoint = endpoint(
    delete(path / "user" / segment[String]()),
    ok(emptyResponse)
  )

  val emptyResponseSmokeEndpoint = endpoint(
    get(
      path / "user" / segment[String]() / "description" /? (qs[String]("name") & qs[
        Int
      ]("age"))
    ),
    ok(emptyResponse)
  )

  val optionalEndpoint: Endpoint[Unit, Option[String]] = endpoint(
    get(path / "users" / "1"),
    wheneverFound(ok(textResponse))
  )

  val headers1 = requestHeader("A") ++ requestHeader("B")
  val joinedHeadersEndpoint = endpoint(
    get(path / "joinedHeadersEndpoint", headers = headers1),
    ok(textResponse)
  )

  val headers2 = requestHeader("C").xmapPartial(s =>
    Validated.fromTry(Try(s.toInt)).mapErrors(_ => Seq(s"Invalid integer: $s"))
  )(_.toString())
  val xmapHeadersEndpoint = endpoint(
    get(path / "xmapHeadersEndpoint", headers = headers2),
    ok(textResponse)
  )

  val url1 = (path / "xmapUrlEndpoint" / segment[Long](): Url[Long])
    .xmap(_.toString())(_.toLong)
  val xmapUrlEndpoint = endpoint(
    get(url1),
    ok(textResponse)
  )

  val dateTimeFormatter = DateTimeFormatter.ISO_DATE
  val reqBody1 = textRequest.xmapWithCodec(
    Codec.parseStringCatchingExceptions(
      `type` = "date",
      parse = LocalDate.parse(_, dateTimeFormatter),
      print = dateTimeFormatter.format(_)
    )
  )
  val xmapReqBodyEndpoint = endpoint(
    post(path / "xmapReqBodyEndpoint", reqBody1),
    ok(textResponse)
  )

  val optUUIDQsEndpoint = endpoint(
    get(
      path / "user" / segment[String]() / "whatever" /? (qs[UUID]("id") & qs[
        Option[Int]
      ]("age"))
    ),
    ok(textResponse)
  )

  val optQsEndpoint = endpoint(
    get(
      path / "user" / segment[String]() / "whatever" /? (qs[String]("name") & qs[
        Option[Int]
      ]("age"))
    ),
    ok(textResponse)
  )

  case class Cache(
      etag: String,
      lastModified: String /* I couldn’t find how to parse these dates */
  )

  val cacheHeaders: ResponseHeaders[Cache] =
    (responseHeader("ETag") ++ responseHeader("Last-Modified"))
      .xmap {
        case (etag, lastModified) => Cache(etag, lastModified)
      }(cache => (cache.etag, cache.lastModified))

  val versionedResource = endpoint(
    get(path / "versioned-resource"),
    ok(textResponse, headers = cacheHeaders)
  )

  val endpointWithOptionalResponseHeader = endpoint(
    get(path / "maybe-cors-enabled"),
    ok(textResponse, headers = optResponseHeader("Access-Control-Allow-Origin"))
  )

  val transformedRequest =
    get(
      url = path / "transformed-request" /? qs[Int]("n"),
      headers = requestHeader("Accept")
    ).xmapPartial {
      case (queryParam, headerValue) =>
        if (headerValue.length == queryParam) Valid((queryParam, headerValue))
        else
          Invalid(
            "Invalid combination of request header and query string parameter"
          )
    }(identity)

  val endpointWithTransformedRequest = endpoint(
    transformedRequest,
    ok(emptyResponse)
  )

  case class StringWrapper(str: String)

  val transformedResponseEntity =
    textResponse.xmap(StringWrapper)(_.str)

  val endpointWithTransformedResponseEntity = endpoint(
    get(path / "transformed-response-entity"),
    ok(transformedResponseEntity)
  )

  case class TransformedResponse(entity: String, etag: String)

  val endpointWithTransformedResponse = endpoint(
    get(path / "transformed-response"),
    ok(
      entity = textResponse,
      headers = responseHeader("ETag")
    ).xmap(TransformedResponse.tupled)(r => (r.entity, r.etag))
  )

}
