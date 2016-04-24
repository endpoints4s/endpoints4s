package endpoints

import scala.language.higherKinds

// TODO At some point, we will probably split this file into several smaller ones (`RequestsAlg`, `ResponsesAlg`, etc.)
trait EndpointsAlg {

  type Segment[A]

  implicit def stringSegment: Segment[String]

  implicit def intSegment: Segment[Int]


  type QueryString[A] <: QueryStringOps[A]

  trait QueryStringOps[A] { first: QueryString[A] =>
    final def & [B](second: QueryString[B])(implicit fc: FlatConcat[A, B]): QueryString[fc.Out] =
      combineQueryStrings(first, second)
  }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit fc: FlatConcat[A, B]): QueryString[fc.Out]

  def qs[A](name: String)(implicit value: QueryStringValue[A]): QueryString[A]

  type QueryStringValue[A]

  implicit def stringQueryString: QueryStringValue[String]

  implicit def intQueryString: QueryStringValue[Int]


  type Path[A] <: PathOps[A] with Url[A]

  trait PathOps[A] { first: Path[A] =>
    final def / (second: String): Path[A] = chainPaths(first, staticPathSegment(second))
    final def / [B](second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out] = chainPaths(first, second)
    final def /? [B](qs: QueryString[B])(implicit fc: FlatConcat[A, B]): Url[fc.Out] = urlWithQueryString(first, qs)
  }

  def staticPathSegment(segment: String): Path[Unit]

  def segment[A](implicit s: Segment[A]): Path[A]

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out]

  val path: Path[Unit] = staticPathSegment("")


  type Url[A]

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit fc: FlatConcat[A, B]): Url[fc.Out]


  type Request[A]

  type RequestEntity[A]

  def get[A](url: Url[A]): Request[A]

  def post[A, B](url: Url[A], entity: RequestEntity[B])(implicit fc: FlatConcat[A, B]): Request[fc.Out]

  def jsonRequest[A : JsonRequest]: RequestEntity[A]


  type Response[A]

  def emptyResponse: Response[Unit]

  def jsonResponse[A : JsonResponse]: Response[A]


  type JsonResponse[A]

  type JsonRequest[A]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B]

  type Endpoint[A, B]

}
