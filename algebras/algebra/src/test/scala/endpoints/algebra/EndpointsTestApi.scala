package endpoints.algebra

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.UUID

import endpoints.algebra

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

}
