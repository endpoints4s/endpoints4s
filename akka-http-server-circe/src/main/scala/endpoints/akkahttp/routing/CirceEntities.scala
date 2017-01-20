package endpoints.akkahttp.routing

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import de.heikoseeberger.akkahttpcirce.CirceSupport
import endpoints.algebra
import endpoints.algebra.CirceEntities.CirceCodec
import io.circe.jawn

/**
  * Implements [[algebra.CirceEntities]] for [[Endpoints]].
  */
trait CirceEntities extends Endpoints with algebra.CirceEntities with CirceSupport {

  def jsonRequest[A : CirceCodec] = {
    implicit val decoder = implicitly[CirceCodec[A]].decoder
    Directives.entity[A](implicitly[FromRequestUnmarshaller[A]])
  }

  def jsonResponse[A : CirceCodec] = a => {
    implicit val decoder = implicitly[CirceCodec[A]].encoder
    Directives.complete(a)
  }

}
