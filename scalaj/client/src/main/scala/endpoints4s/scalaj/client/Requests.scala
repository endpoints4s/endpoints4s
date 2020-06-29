package endpoints4s.scalaj.client

import endpoints4s.{
  PartialInvariantFunctor,
  Semigroupal,
  Tupler,
  Validated,
  algebra
}
import endpoints4s.algebra.Documentation
import scalaj.http.HttpRequest

/**
  * @group interpreters
  */
trait Requests extends algebra.Requests with Urls with Methods {

  type RequestHeaders[A] = A => Seq[(String, String)]

  type Request[A] = A => HttpRequest

  implicit def requestPartialInvariantFunctor
      : PartialInvariantFunctor[Request] =
    new PartialInvariantFunctor[Request] {
      def xmapPartial[A, B](
          fa: Request[A],
          f: A => Validated[B],
          g: B => A
      ): Request[B] =
        fa compose g
    }

  type RequestEntity[A] = (A, HttpRequest) => HttpRequest

  def emptyRequestHeaders: RequestHeaders[Unit] = _ => Seq()

  def requestHeader(
      name: String,
      docs: Documentation
  ): String => Seq[(String, String)] = value => Seq(name -> value)

  def optRequestHeader(
      name: String,
      docs: Documentation
  ): Option[String] => Seq[(String, String)] =
    valueOpt => valueOpt.map(v => name -> v).toSeq

  implicit def requestHeadersPartialInvariantFunctor
      : PartialInvariantFunctor[RequestHeaders] =
    new PartialInvariantFunctor[RequestHeaders] {
      def xmapPartial[From, To](
          f: RequestHeaders[From],
          map: From => Validated[To],
          contramap: To => From
      ): RequestHeaders[To] =
        to => f(contramap(to))
    }

  implicit def requestHeadersSemigroupal: Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {
      override def product[A, B](
          fa: A => Seq[(String, String)],
          fb: B => Seq[(String, String)]
      )(implicit tupler: Tupler[A, B]): tupler.Out => Seq[(String, String)] =
        out => {
          val (a, b) = tupler.unapply(out)
          fa(a) ++ fb(b)
        }
    }

  def emptyRequest: RequestEntity[Unit] = (x: Unit, req: HttpRequest) => req

  def textRequest: (String, HttpRequest) => scalaj.http.HttpRequest =
    (body, req) =>
      req
        .header("content-type", s"text/plain; charset=${req.charset}")
        .postData(body)

  def choiceRequestEntity[A, B](
      requestEntityA: RequestEntity[A],
      requestEntityB: RequestEntity[B]
  ): RequestEntity[Either[A, B]] =
    (eitherAB, req) =>
      eitherAB.fold(requestEntityA(_, req), requestEntityB(_, req))

  implicit def requestEntityPartialInvariantFunctor
      : PartialInvariantFunctor[RequestEntity] =
    new PartialInvariantFunctor[RequestEntity] {
      def xmapPartial[A, B](
          fa: RequestEntity[A],
          f: A => Validated[B],
          g: B => A
      ): RequestEntity[B] =
        (to, req) => fa(g(to), req)
    }

  def request[U, E, H, UE, Out](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E] = emptyRequest,
      docs: Documentation = None,
      headers: RequestHeaders[H] = emptyRequestHeaders
  )(
      implicit tuplerUE: Tupler.Aux[U, E, UE],
      tuplerUEH: Tupler.Aux[UE, H, Out]
  ): Request[Out] =
    (ueh) => {
      val (ue, h) = tuplerUEH.unapply(ueh)
      val (u, e) = tuplerUE.unapply(ue)
      val req = url
        .toReq(u)
        .headers(headers(h))
      entity(e, req)
        .method(method)
    }

}
