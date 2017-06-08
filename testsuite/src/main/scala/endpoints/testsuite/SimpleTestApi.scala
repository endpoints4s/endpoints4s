package endpoints.testsuite

import endpoints.algebra
import endpoints.algebra.Builders

trait SimpleTestApi extends algebra.Endpoints with Builders {


  val smokeEndpoint = endpoint(
    get(path / "user" / segment[String] / "description" /? (qs[String]("name") & qs[Int]("age"))),
    textResponse
  )

  val smokeEndpointViaBuilder =
    anEndpoint
      .withMethod(Get)
      .withUrl(path / "user" / segment[String] / "description" /? (qs[String]("name") & qs[Int]("age")))
      .withTextResponse
      .build

}
