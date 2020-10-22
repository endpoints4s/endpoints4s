package endpoints4s.openapi

import endpoints4s.openapi.model.{MediaType, Schema}
import endpoints4s.ujson.codecs.schemas
import endpoints4s.{Invalid, algebra}

/** @group interpreters
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
