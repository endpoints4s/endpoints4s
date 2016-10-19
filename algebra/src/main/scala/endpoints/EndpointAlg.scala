package endpoints

import scala.language.higherKinds

/**
  * Algebra interface for defining endpoints made of requests and responses.
  *
  * Requests and responses contain headers and entity.
  */
trait EndpointAlg extends UrlAlg {

  type RequestHeaders[A]

  def emptyHeaders: RequestHeaders[Unit]


  type Request[A]

  type RequestEntity[A]

  def get[A, B](url: Url[A], headers: RequestHeaders[B] = emptyHeaders)(implicit tupler: Tupler[A, B]): Request[tupler.Out]

  def post[A, B, C, AB](url: Url[A], entity: RequestEntity[B], headers: RequestHeaders[C] = emptyHeaders)(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out]


  type Response[A]

  def emptyResponse: Response[Unit]


  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B]

  type Endpoint[A, B]

}
