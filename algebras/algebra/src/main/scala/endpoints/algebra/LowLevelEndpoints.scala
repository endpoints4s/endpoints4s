package endpoints.algebra

/**
  * Provides a way to define endpoints that directly expose the low level APIs of the
  * interpreters.
  *
  * Using this trait is not recommended because endpoints defined using these methods
  * miss the opportunity to share a consistent protocol between client and server
  * interpreters. However, it can be useful for transitioning legacy code.
  *
  * Example of endpoint definition:
  *
  * {{{
  *   val someEndpoint = endpoint(post(path, rawRequestEntity), rawResponseEntity)
  * }}}
  *
  * Endpoint implementation:
  *
  * {{{
  *   someEndpoint.implementedBy { request =>
  *     Ok(request.body.asText.getOrElse("Unable to decode request entity"))
  *   }
  * }}}
  *
  * XMLHttpRequest call:
  *
  * {{{
  *   someEndpoint(xhr => "Foo")
  *     .map(response => println(response.responseText))
  * }}}
  */
trait LowLevelEndpoints extends Endpoints {

  /** Low-level request entity */
  type RawRequestEntity

  def rawRequestEntity: RequestEntity[RawRequestEntity]

  /** Low-level request response */
  type RawResponseEntity

  def rawResponseEntity: Response[RawResponseEntity]

}
