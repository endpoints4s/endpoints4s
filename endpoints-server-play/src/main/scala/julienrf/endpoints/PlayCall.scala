package julienrf.endpoints

import play.api.mvc.Call

class PlayCall extends Endpoints {

  type Path[A] = A => String

  def static(segment: String) = _ => segment

  def dynamic = (s: String) => s

  def chained[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out] =
    (ab: fc.Out) => {
      val (a, b) = fc.unapply(ab)
      first(a) ++ "/" ++ second(b)
    }


  type Request[A] = A => Call

  type RequestMarshaller[A] = Unit

  def get[A](path: Path[A]) =
    a => Call("GET", path(a))


  type Response[A] = Unit

  type ResponseMarshaller[A] = Unit

  def jsonEntity[A](implicit ev: ResponseMarshaller[A]) = ()


  type Endpoint[I, O] = I => Call

  def endpoint[A, B](request: Request[A], response: Response[B]) = request

}

