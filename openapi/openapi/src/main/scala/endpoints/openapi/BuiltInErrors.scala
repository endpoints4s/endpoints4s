package endpoints.openapi

import endpoints.openapi.model.{MediaType, Schema}
import endpoints.ujson.codecs.schemas
import endpoints.{Invalid, algebra}

/**
  * @group interpreters
  */
trait BuiltInErrors extends algebra.BuiltInErrors {
  this: EndpointsWithCustomErrors =>

  private lazy val invalidJsonEntity =
    Map(
      "application/json" -> MediaType(
        Some(
          Schema.Reference(
            "endpoints.Errors",
            Some(schemas.toSchema(schemas.invalid.docs)),
            None
          )
        )
      )
    )

  lazy val clientErrorsResponseEntity: ResponseEntity[Invalid] =
    invalidJsonEntity

  lazy val serverErrorResponseEntity: ResponseEntity[Throwable] =
    invalidJsonEntity

}
