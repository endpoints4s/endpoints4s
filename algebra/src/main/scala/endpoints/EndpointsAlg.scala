package endpoints

import scala.language.higherKinds

// TODO At some point, we will probably split this file into several smaller ones (`RequestsAlg`, `ResponsesAlg`, etc.)
trait EndpointsAlg {

  type Path[A] <: PathOps[A]

  trait PathOps[A] { first: Path[A] =>
    final def / (second: String): Path[A] = chainedPaths(first, staticPathSegment(second))
    final def / [B](second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out] = chainedPaths(first, second)
  }

  def staticPathSegment(segment: String): Path[Unit]

  // TODO Path codecs
  def dynamicPathSegment: Path[String]

  def chainedPaths[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out]

  val path: Path[Unit] = staticPathSegment("")

  // TODO Query string

  type Request[A]

  type RequestEntity[A]

  def get[A](path: Path[A]): Request[A]

  def post[A, B](path: Path[A], entity: RequestEntity[B])(implicit fc: FlatConcat[A, B]): Request[fc.Out]

  def jsonRequest[A : JsonRequest]: RequestEntity[A]


  type Response[A]

  def emptyResponse: Response[Unit]

  def jsonResponse[A : JsonResponse]: Response[A]


  type JsonResponse[A]

  type JsonRequest[A]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B]

  type Endpoint[A, B]

}
