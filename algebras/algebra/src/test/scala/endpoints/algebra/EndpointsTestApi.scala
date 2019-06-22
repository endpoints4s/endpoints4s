package endpoints.algebra

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.UUID

import endpoints.algebra

trait EndpointsTestApi extends algebra.Endpoints {


  val UUIDEndpoint = endpoint(
    get(path / "user" / segment[UUID]() / "description" /? (qs[String]("name") & qs[Int]("age"))),
    textResponse()
  )

  val putUUIDEndpoint = endpoint(
    put(path / "user" / segment[UUID](), emptyRequest),
    emptyResponse()
  )

  val deleteUUIDEndpoint = endpoint(
    delete(path / "user" / segment[UUID]()),
    emptyResponse()
  )

  val emptyResponseUUIDEndpoint = endpoint(
    get(path / "user" / segment[UUID]() / "description" /? (qs[String]("name") & qs[Int]("age"))),
    emptyResponse()
  )

  val smokeEndpoint = endpoint(
    get(path / "user" / segment[String]() / "description" /? (qs[String]("name") & qs[Int]("age"))),
    textResponse()
  )

  val putEndpoint = endpoint(
    put(path / "user" / segment[String](), emptyRequest),
    emptyResponse()
  )

  val deleteEndpoint = endpoint(
    delete(path / "user" / segment[String]()),
    emptyResponse()
  )

  val emptyResponseSmokeEndpoint = endpoint(
    get(path / "user" / segment[String]() / "description" /? (qs[String]("name") & qs[Int]("age"))),
    emptyResponse()
  )

  val optionalEndpoint: Endpoint[Unit, Option[String]] = endpoint(
    get(path / "users" / "1"),
    wheneverFound(textResponse())
  )

  val headers1 = header("A") ++ header("B")
  val joinedHeadersEndpoint = endpoint(
    get(path / "joinedHeadersEndpoint", headers1),
    textResponse()
  )

  val headers2 = header("C").xmap(_.toInt)(_.toString)
  val xmapHeadersEndpoint = endpoint(
    get(path / "xmapHeadersEndpoint", headers2),
    textResponse()
  )

  val url1 = (path / "xmapUrlEndpoint" / segment[Long]() : Url[Long]).xmap(_.toString)( _.toLong)
  val xmapUrlEndpoint = endpoint(
    get(url1),
    textResponse()
  )

  val dateTimeFormatter = DateTimeFormatter.ISO_DATE
  val reqBody1 = textRequest().xmap(s => LocalDate.parse(s, dateTimeFormatter))( d => dateTimeFormatter.format(d))
  val xmapReqBodyEndpoint = endpoint(
    post(path / "xmapReqBodyEndpoint", reqBody1),
    textResponse()
  )

  val optUUIDQsEndpoint = endpoint(
    get(path / "user" / segment[String]() / "whatever" /? (qs[UUID]("id") & qs[Option[Int]]("age"))),
    textResponse()
  )

  val optQsEndpoint = endpoint(
    get(path / "user" / segment[String]() / "whatever" /? (qs[String]("name") & qs[Option[Int]]("age"))),
    textResponse()
  )

}
