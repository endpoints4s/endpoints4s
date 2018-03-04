package endpoints.testsuite

import endpoints.algebra

trait SimpleTestApi extends algebra.Endpoints {


  val smokeEndpoint = endpoint(
    get(path / "user" / segment[String] / "description" /? (qs[String]("name") & qs[Int]("age"))),
    textResponse
  )


  val emptyResponseSmokeEndpoint = endpoint(
    get(path / "user" / segment[String] / "description" /? (qs[String]("name") & qs[Int]("age"))),
    emptyResponse
  )

}
