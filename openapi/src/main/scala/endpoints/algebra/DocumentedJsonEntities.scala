package endpoints
package algebra

import scala.language.higherKinds

/**
  * Algebra interface for describing json entities including documentation.
  *
  * This interface is modeled after [[JsonEntities]] but some methods take
  * additional parameters carrying documentation.
  */
trait DocumentedJsonEntities extends DocumentedEndpoints {

  /** Type class defining how to represent the `A` information as a JSON request entity */
  type JsonRequest[A]

  /** Defines a `RequestEntity[A]` given an implicit `JsonRequest[A]` */
  def jsonRequest[A : JsonRequest](description: Option[String] = None): RequestEntity[A]


  /** Type class defining how to represent the `A` information as a JSON response entity */
  type JsonResponse[A]

  /** Defines a `Response[A]` given an implicit `JsonResponse[A]` */
  def jsonResponse[A : JsonResponse](description: String): Response[A]

}
