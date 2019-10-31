package endpoints.openapi

import endpoints.{Invalid, algebra}
import endpoints.openapi.model.{MediaType, Schema}

trait BuiltInErrors extends algebra.BuiltInErrors { this: EndpointsWithCustomErrors =>

  private lazy val invalidJsonEntity =
    Map("application/json" -> MediaType(Some(Schema.Reference("endpoints.Errors", Some(Schema.Array(Left(Schema.simpleString), None)), None))))

  lazy val clientErrorsResponseEntity: ResponseEntity[Invalid] = invalidJsonEntity

  lazy val serverErrorResponseEntity: ResponseEntity[Throwable] = invalidJsonEntity

}
