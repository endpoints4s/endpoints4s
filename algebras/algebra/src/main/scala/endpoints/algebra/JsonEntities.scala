package endpoints.algebra

/**
  * Algebra interface for describing JSON entities in requests and responses.
  *
  * Generally, this algebra is not directly used, but one of its specialized algebras
  * is used instead: [[JsonEntitiesFromSchemas]] or [[JsonEntitiesFromCodecs]].
  *
  * {{{
  *   /**
  *     * Describes an HTTP endpoint whose:
  *     *  - request uses verb “GET”,
  *     *  - URL is made of the segment “/user” followed by a `String` segment,
  *     *  - response content type is JSON and contains a `User`
  *     */
  *   val example = endpoint(get(path / "user" / segment[UUID]), jsonResponse[User])
  * }}}
  *
  * @group algebras
  */
trait JsonEntities extends EndpointsWithCustomErrors {

  /** Type class defining how to represent the `A` information as a JSON request entity
    * @group types */
  type JsonRequest[A]

  /** Type class defining how to represent the `A` information as a JSON response entity
    * @group types */
  type JsonResponse[A]

  /**
    * Request with a JSON body, given an implicit `JsonRequest[A]`
    *
    *   - Server interpreters accept requests with content-type `application/json` and
    *     reject requests with an incorrect content-type.
    *   - Client interpreters supply content-type `application/json`
    *
    * @group operations
    */
  def jsonRequest[A: JsonRequest]: RequestEntity[A]

  /** Defines a `Response[A]` given an implicit `JsonResponse[A]`
    * @group operations */
  def jsonResponse[A: JsonResponse]: ResponseEntity[A]
}

/**
  * Fixes both the `JsonRequest` and `JsonResponse` types to be a same `JsonCodec` type.
  *
  * This trait is used as an implementation detail (to reuse code between [[JsonEntitiesFromSchemas]]
  * and [[JsonEntitiesFromCodecs]]) and is not useful to end-users.
  *
  * @group algebras
  */
trait JsonCodecs extends JsonEntities {

  type JsonRequest[A] = JsonCodec[A]
  type JsonResponse[A] = JsonCodec[A]

  /** A JSON codec type class
    * @group types */
//#json-codec-type
  type JsonCodec[A]
//#json-codec-type

}

/**
  * Turns a `JsonCodec` into a [[Codec]].
  *
  * @group algebras
  */
trait JsonEntitiesFromCodecs extends JsonCodecs {

  /** Turns a JsonCodec[A] into a Codec[String, A]
    * @group operations */
  def stringCodec[A: JsonCodec]: Codec[String, A]

}

/**
  * Partially applies the [[JsonEntities]] algebra interface to fix the
  * `JsonRequest` and `JsonResponse` types to be `JsonSchema`.
  *
  * @group algebras
  */
trait JsonEntitiesFromSchemas extends JsonCodecs with JsonSchemas {

//#type-carrier
  type JsonCodec[A] = JsonSchema[A]
//#type-carrier

}
