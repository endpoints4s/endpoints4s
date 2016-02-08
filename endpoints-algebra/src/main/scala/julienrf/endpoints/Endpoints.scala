package julienrf.endpoints

import scala.language.higherKinds

trait Endpoints {

  type Path[A]

  def static(segment: String): Path[Unit]

  def dynamic: Path[String]

  def chained[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out]


  type Request[A]

  def get[A](path: Path[A]): Request[A]


  type Response[A]

  def jsonEntity[A](implicit O: ResponseMarshaller[A]): Response[A]


  type Endpoint[I, O]

  type RequestMarshaller[A]

  type ResponseMarshaller[A]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B]

}
