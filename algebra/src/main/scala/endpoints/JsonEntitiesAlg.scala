package endpoints

import scala.language.higherKinds

trait JsonEntitiesAlg extends EndpointsAlg {

  type JsonRequest[A]

  def jsonRequest[A : JsonRequest]: RequestEntity[A]


  type JsonResponse[A]

  def jsonResponse[A : JsonResponse]: Response[A]

}
