package endpoints4s.xhr

import endpoints4s.algebra
import org.scalajs.dom.XMLHttpRequest

import scala.scalajs.js

/** Interpreter for [[algebra.LowLevelEndpoints]] that represents the response as a
  * `XMLHttpRequest` value.
  */
trait LowLevelEndpoints extends algebra.LowLevelEndpoints with EndpointsWithCustomErrors {

  /** Represents the request entity as a function that is passed the underlying XMLHttpRequest (so this one can be modified in place) and returns the actual entity to use */
  type RawRequestEntity = js.Function1[XMLHttpRequest, js.Any]

  /** Sends the entity as it is */
  lazy val rawRequestEntity: RequestEntity[RawRequestEntity] = (entity, xhr) => entity(xhr)

  /** The response can be retrieved from the XMLHttpRequest API */
  type RawResponseEntity = XMLHttpRequest

  /** Successfully returns the underlying XMLHttpRequest, whatever its status code is */
  lazy val rawResponseEntity: Response[RawResponseEntity] = xhr => Some(_ => Right(xhr))

}
