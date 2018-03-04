package endpoints.testsuite

import endpoints.algebra

trait OptionalResponsesTestApi extends algebra.Endpoints with algebra.OptionalResponses {


  val optionalEndpoint: Endpoint[Unit, Option[String]] = endpoint(
    get[Unit, Unit](path / "users" / "1"),
    option(textResponse)
  )

}
