package julienrf.endpoints

import scala.language.higherKinds

trait Endpoints extends EndpointType {

  type Path[A] <: PathOps[A]

  trait PathOps[A] { first: Path[A] =>
    final def / (second: String): Path[A] = chained(first, static(second))
    final def / [B](second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out] = chained(first, second)
  }

  def static(segment: String): Path[Unit]

  def dynamic: Path[String]

  def chained[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out]

  val path: Path[Unit] = static("")

  type Request[A]

  type RequestEntity[A]

  def get[A](path: Path[A]): Request[A]

  def post[A, B](path: Path[A], entity: RequestEntity[B])(implicit fc: FlatConcat[A, B]): Request[fc.Out]

  val request: RequestApi

  trait RequestApi {
    def jsonEntity[A : RequestMarshaller]: RequestEntity[A]
  }


  type Response[A]

  def jsonEntity[A](implicit O: ResponseMarshaller[A]): Response[A]


  type RequestMarshaller[A]

  type ResponseMarshaller[A]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B]

}
