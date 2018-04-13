package endpoints.algebra

import endpoints.algebra

trait EndpointsTestApi extends algebra.Endpoints {


  val smokeEndpoint = endpoint(
    get(path / "user" / segment[String]() / "description" /? (qs[String]("name") & qs[Int]("age"))),
    textResponse()
  )


  val emptyResponseSmokeEndpoint = endpoint(
    get(path / "user" / segment[String]() / "description" /? (qs[String]("name") & qs[Int]("age"))),
    emptyResponse()
  )

  val optionalEndpoint: Endpoint[Unit, Option[String]] = endpoint(
    get[Unit, Unit](path / "users" / "1"),
    option(textResponse())
  )

  val headers1 = header("A") ++ header("B")
  val joinedHeadersEndpoint = endpoint(
    get(path / "joinedHeadersEndpoint", headers1),
    textResponse()
  )

  val headers2 = header("C").xmap[Int](_.toInt, _.toString)
  val xmapHeadersEndpoint = endpoint(
    get(path / "xmapHeadersEndpoint", headers2),
    textResponse()
  )

//  val url1 = (path / segment[Long]()).xmap[String](_.toString, _.toLong)


}
