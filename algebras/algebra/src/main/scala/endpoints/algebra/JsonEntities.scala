package endpoints.algebra

/**
  * Algebra interface for describing JSON entities in requests and responses.
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

  /** Type class defining how to represent the `A` information as a JSON request entity */
  type JsonRequest[A]

  /** Type class defining how to represent the `A` information as a JSON response entity */
  type JsonResponse[A]

  /** Defines a `RequestEntity[A]` given an implicit `JsonRequest[A]` */
  def jsonRequest[A : JsonRequest]: RequestEntity[A]

  /** Defines a `Response[A]` given an implicit `JsonResponse[A]` */
  def jsonResponse[A : JsonResponse]: ResponseEntity[A]
}

/**
  * Fixes both the `JsonRequest` and `JsonResponse` types to be a same `JsonCodec` type.
  *
  * @group algebras
  */
trait JsonCodecs extends JsonEntities {

  type JsonRequest[A] = JsonCodec[A]
  type JsonResponse[A] = JsonCodec[A]

//#json-codec-type
  /** A JSON codec type class */
  type JsonCodec[A]
//#json-codec-type

}

/**
  * Turns a `JsonCodec` into a [[Codec]].
  *
  * @group algebras
  */
trait JsonEntitiesFromCodec extends JsonCodecs {

  /** Turns a JsonCodec[A] into a Codec[String, A] */
  def stringCodec[A : JsonCodec]: Codec[String, A]

}
