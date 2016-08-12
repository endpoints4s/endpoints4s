package endpoints

import scala.language.higherKinds

trait JsonEntityAlg extends EndpointAlg {

  type JsonRequest[A]

  def jsonRequest[A : JsonRequest]: RequestEntity[A]


  type JsonResponse[A]

  def jsonResponse[A : JsonResponse]: Response[A]

}
