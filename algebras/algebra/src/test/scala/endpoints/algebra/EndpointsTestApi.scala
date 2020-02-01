package endpoints.algebra

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.UUID

import endpoints.{Invalid, Valid, algebra}

trait EndpointsTestApi extends algebra.Endpoints {


  val UUIDEndpoint = endpoint(
    get(path / "user" / segment[UUID]() / "description" /? (qs[String]("name") & qs[Int]("age"))),
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
    get(path / "user" / segment[UUID]() / "description" /? (qs[String]("name") & qs[Int]("age"))),
    ok(emptyResponse)
  )

  val smokeEndpoint = endpoint(
    get(path / "user" / segment[String]() / "description" /? (qs[String]("name") & qs[Int]("age"))),
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
    get(path / "user" / segment[String]() / "description" /? (qs[String]("name") & qs[Int]("age"))),
    ok(emptyResponse)
  )

  val optionalEndpoint: Endpoint[Unit, Option[String]] = endpoint(
    get(path / "users" / "1"),
    wheneverFound(ok(textResponse))
  )

  val headers1 = header("A") ++ header("B")
  val joinedHeadersEndpoint = endpoint(
    get(path / "joinedHeadersEndpoint", headers = headers1),
    ok(textResponse)
  )

  val headers2 = header("C").xmap(_.toInt)(_.toString)
  val xmapHeadersEndpoint = endpoint(
    get(path / "xmapHeadersEndpoint", headers = headers2),
    ok(textResponse)
  )

  val url1 = (path / "xmapUrlEndpoint" / segment[Long]() : Url[Long]).xmap(_.toString)( _.toLong)
  val xmapUrlEndpoint = endpoint(
    get(url1),
    ok(textResponse)
  )

  val dateTimeFormatter = DateTimeFormatter.ISO_DATE
  val reqBody1 = textRequest.xmap(s => LocalDate.parse(s, dateTimeFormatter))( d => dateTimeFormatter.format(d))
  val xmapReqBodyEndpoint = endpoint(
    post(path / "xmapReqBodyEndpoint", reqBody1),
    ok(textResponse)
  )

  val optUUIDQsEndpoint = endpoint(
    get(path / "user" / segment[String]() / "whatever" /? (qs[UUID]("id") & qs[Option[Int]]("age"))),
    ok(textResponse)
  )

  val optQsEndpoint = endpoint(
    get(path / "user" / segment[String]() / "whatever" /? (qs[String]("name") & qs[Option[Int]]("age"))),
    ok(textResponse)
  )

  case class Cache(etag: String, lastModified: String /* I couldn’t find how to parse these dates */)

  val cacheHeaders: ResponseHeaders[Cache] =
    (responseHeader("ETag") ++ responseHeader("Last-Modified"))
      .xmapPartial { case (etag, lastModified) =>
        val validDate =
          if (lastModified.contains("GMT")) Valid(lastModified)
          else Invalid("Invalid date")
        validDate.map(Cache(etag, _))
      } (cache => (cache.etag, cache.lastModified))

  val versionedResource = endpoint(
    get(path / "versioned-resource"),
    ok(textResponse, headers = cacheHeaders)
  )

  val endpointWithOptionalResponseHeader = endpoint(
    get(path / "maybe-cors-enabled"),
    ok(textResponse, headers = optResponseHeader("Access-Control-Allow-Origin"))
  )

}
