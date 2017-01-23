package endpoints.akkahttp.routing

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import de.heikoseeberger.akkahttpcirce.CirceSupport
import endpoints.algebra
import endpoints.algebra.CirceEntities.CirceCodec

/**
  * Implements [[algebra.CirceEntities]] for [[Endpoints]].
  */
trait CirceEntities extends Endpoints with algebra.CirceEntities {

  import CirceSupport._

  def jsonRequest[A : CirceCodec] = {
    implicit val decoder = CirceCodec[A].decoder
    val frum = implicitly[FromRequestUnmarshaller[A]]
    Directives.entity[A](frum)
  }

  def jsonResponse[A : CirceCodec] = a => {
    implicit val decoder = implicitly[CirceCodec[A]].encoder
    Directives.complete(a)
  }

}
