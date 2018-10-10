package endpoints.scalaj.client

import endpoints.{InvariantFunctor, Semigroupal, Tupler, algebra}
import endpoints.algebra.Documentation

import scalaj.http.HttpRequest

trait Requests extends algebra.Requests with Urls with Methods {


   type RequestHeaders[A] = A => Seq[(String, String)]

  type Request[A] = A => HttpRequest

  type RequestEntity[A] = (A, HttpRequest) => HttpRequest

  def emptyHeaders: RequestHeaders[Unit] = _ => Seq()

  def header(name: String, docs: Documentation): String => Seq[(String, String)] = value => Seq(name -> value)

  def optHeader(name: String, docs: Documentation): Option[String] => Seq[(String, String)] =
    valueOpt => valueOpt.map(v => name -> v).toSeq

  implicit def reqHeadersInvFunctor: InvariantFunctor[RequestHeaders] = new InvariantFunctor[RequestHeaders] {
    override def xmap[From, To](f: From => Seq[(String, String)], map: From => To, contramap: To => From): To => Seq[(String, String)] =
      to => f(contramap(to))
  }

  implicit def reqHeadersSemigroupal: Semigroupal[RequestHeaders] = new Semigroupal[RequestHeaders] {
    override def product[A, B](fa: A => Seq[(String, String)], fb: B => Seq[(String, String)])(implicit tupler: Tupler[A, B]): tupler.Out => Seq[(String, String)] =
      out => {
        val (a, b) = tupler.unapply(out)
        fa(a) ++ fb(b)
      }
  }


  def emptyRequest: RequestEntity[Unit] = (x: Unit, req: HttpRequest) => req

  def textRequest(docs: Option[String]): (String, HttpRequest) => scalaj.http.HttpRequest =
    (body, req) => req.postData(body)

  implicit def reqEntityInvFunctor: InvariantFunctor[RequestEntity] = new InvariantFunctor[RequestEntity] {
    override def xmap[From, To](f: (From, HttpRequest) => HttpRequest, map: From => To, contramap: To => From): (To, HttpRequest) => HttpRequest =
      (to, req) => f(contramap(to), req)
  }


  def request[U, E, H, UE, Out](method: Method,
    url: Url[U],
    entity: RequestEntity[E] = emptyRequest,
    headers: RequestHeaders[H] = emptyHeaders
  )(implicit tuplerUE: Tupler.Aux[U, E, UE], tuplerUEH: Tupler.Aux[UE, H, Out]): Request[Out] =
    (ueh) => {
      val (ue, h) = tuplerUEH.unapply(ueh)
      val (u, e) = tuplerUE.unapply(ue)
      val req = url.toReq(u)
        .headers(headers(h))
      entity(e, req)
        .method(method)
    }

}
