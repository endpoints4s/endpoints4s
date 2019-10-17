package endpoints.xhr.circe

import endpoints.{algebra, circe, xhr}
import io.circe.parser
import org.scalajs.dom.XMLHttpRequest

/**
  * Interpreter for `JsonSchemaEntities` that uses the circe `JsonSchemas` interpreter.
  */
trait JsonSchemaEntities
  extends xhr.EndpointsWithCustomErrors
    with algebra.JsonSchemaEntities
    with circe.JsonSchemas {

  def jsonRequest[A](implicit codec: JsonSchema[A]): RequestEntity[A] =
    { (a: A, xhr: XMLHttpRequest) =>
      xhr.setRequestHeader("Content-Type", "application/json")
      codec.encoder.apply(a).noSpaces
    }

  def jsonResponse[A](implicit codec: JsonSchema[A]): ResponseEntity[A] =
    xhr => parser.parse(xhr.responseText).right.flatMap(codec.decoder.decodeJson)

}
