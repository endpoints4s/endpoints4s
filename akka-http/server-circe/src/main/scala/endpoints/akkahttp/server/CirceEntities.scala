package endpoints.akkahttp.server

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import endpoints.algebra
import endpoints.algebra.CirceEntities.CirceCodec

/**
  * Implements [[algebra.CirceEntities]] for [[Endpoints]].
  */
trait CirceEntities extends Endpoints with algebra.CirceEntities {

  import FailFastCirceSupport._

  def jsonRequest[A : CirceCodec]: RequestEntity[A] = {
    implicit val decoder = CirceCodec[A].decoder
    Directives.entity[A](implicitly[FromRequestUnmarshaller[A]])
  }

  def jsonResponse[A : CirceCodec]: Response[A] = (a: A) => {
    implicit val encoder = implicitly[CirceCodec[A]].encoder
    Directives.complete(a)
  }

}
