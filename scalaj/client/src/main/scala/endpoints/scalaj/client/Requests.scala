package endpoints.scalaj.client

import endpoints.Tupler
import endpoints.algebra

import scalaj.http.HttpRequest

trait Requests extends algebra.Requests with Urls with Methods{


   type RequestHeaders[A] = A => Seq[(String, String)]

   type Request[A] = A => HttpRequest

   type RequestEntity[A] = (A, HttpRequest) => HttpRequest

   def emptyHeaders: RequestHeaders[Unit] = _ => Seq()

   def emptyRequest: RequestEntity[Unit] = (x: Unit, req: HttpRequest) => req


  def request[U, E, H, UE](method: Method,
                           url: Url[U],
                           entity: RequestEntity[E] = emptyRequest,
                           headers: RequestHeaders[H] = emptyHeaders
                          )(implicit tuplerUE: Tupler.Aux[U, E, UE], tuplerUEH: Tupler[UE, H]): Request[tuplerUEH.Out] =
    (ueh) => {
      val (ue, h) = tuplerUEH.unapply(ueh)
      val (u, e) = tuplerUE.unapply(ue)
      val req = url.toReq(u)
        .headers(headers(h))
      entity(e, req)
        .method(method)
    }

}
