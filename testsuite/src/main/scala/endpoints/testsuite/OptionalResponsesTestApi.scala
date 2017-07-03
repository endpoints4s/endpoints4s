package endpoints.testsuite

import endpoints.algebra
import endpoints.algebra.OptionalResponseBuilders

trait OptionalResponsesTestApi extends algebra.Endpoints with algebra.OptionalResponses with OptionalResponseBuilders {


  val optionalResponseEndp = endpoint(
    request(Get,
      path / "user",
      emptyRequest
    ),
    option(textResponse)
  )

  val optionalResponseEndpViaBuilder =
    anEndpoint
      .withMethod(Get)
      .withUrl(path / "user")
      .withTextResponse
      .withOptionalResponse
      .build

  val optionalResponseEndpViaBuilder2 =
    anEndpoint
      .withMethod(Get)
      .withUrl(path / "user")
      .withOptionalResponse(textResponse)
      .build


}
