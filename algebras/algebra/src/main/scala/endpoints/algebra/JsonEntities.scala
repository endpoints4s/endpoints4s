package endpoints.algebra

import scala.language.higherKinds

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
trait JsonEntities extends Endpoints {

//#request-response-types
  /** Type class defining how to represent the `A` information as a JSON request entity */
  type JsonRequest[A]

  /** Type class defining how to represent the `A` information as a JSON response entity */
  type JsonResponse[A]
//#request-response-types

  /** Defines a `RequestEntity[A]` given an implicit `JsonRequest[A]` */
  def jsonRequest[A : JsonRequest](docs: Documentation = None): RequestEntity[A]

  /** Defines a `Response[A]` given an implicit `JsonResponse[A]` */
  def jsonResponse[A : JsonResponse](docs: Documentation = None): Response[A]
}

/**
  * Fixes both the `JsonRequest` and `JsonResponse` types to be [[Codec]].
  *
  * @group algebras
  */
trait JsonEntitiesFromCodec extends JsonEntities {

  type JsonRequest[A] = Codec[String, A]
  type JsonResponse[A] = Codec[String, A]

//#json-codec-type
  /** A JSON codec type class */
  type JsonCodec[A]
//#json-codec-type

  /** Turns a JsonCodec[A] into a Codec[String, A] */
  implicit def jsonCodec[A : JsonCodec]: Codec[String, A]

}