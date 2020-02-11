package endpoints.openapi

import endpoints.openapi.model.Schema

/**
  * Internal.
  * @group interpreters
  */
trait Headers {

  /**
    * @param value List of request header names (e.g. “Authorization”)
    */
  case class DocumentedHeaders(value: List[DocumentedHeader])

  case class DocumentedHeader(name: String, description: Option[String], required: Boolean, schema: Schema)

}
