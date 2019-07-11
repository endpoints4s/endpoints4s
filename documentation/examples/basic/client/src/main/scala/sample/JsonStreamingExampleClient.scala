package sample

import endpoints.xhr

object JsonStreamingExampleClient
  extends JsonStreamingExample
    with xhr.Http1JsonStreaming
    with xhr.thenable.Endpoints
    with xhr.circe.JsonSchemaEntities
