package endpoints.http4s.server

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets.UTF_8

import endpoints.algebra.Documentation
import endpoints.{PartialInvariantFunctor, Tupler, algebra}
import org.http4s
import org.http4s.Uri

import scala.collection.compat._
import scala.collection.mutable

trait Urls extends algebra.Urls {
  val utf8Name = UTF_8.name()

  type Params = Map[String, Seq[String]]

  type QueryString[A] = Params => Option[A]
  type QueryStringParam[A] = (String, Params) => Option[A]

  trait Url[A] {
    def decodeUrl(uri: http4s.Uri): Option[A]
  }

  trait Path[A] extends Url[A] {
    def decode(paths: List[String]): Option[(A, List[String])]

    final def decodeUrl(uri: http4s.Uri): Option[A] = {
      decode(uri.path.split("/").to[List]).filter(_._2.isEmpty).map(_._1)
    }
  }

  type Segment[A] = String => Option[A]

  /** Concatenates two `QueryString`s */
  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(
      implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
    map =>
      for {
        a <- first(map)
        b <- second(map)
      } yield tupler(a, b)

  def qs[A](name: String, docs: Documentation = None)(
      implicit value: QueryStringParam[A]): QueryString[A] =
    params => value(name, params)

  implicit def optionalQueryStringParam[A](
      implicit param: QueryStringParam[A]): QueryStringParam[Option[A]] =
    (name, params) =>
      params.get(name) match {
        case None    => Some(None)
        case Some(_) => param(name, params).map(Some(_))
    }

  implicit def repeatedQueryStringParam[A, CC[X] <: Iterable[X]](
      implicit param: QueryStringParam[A],
      factory: Factory[A, CC[A]]): QueryStringParam[CC[A]] =
    (name: String, qs: Map[String, Seq[String]]) =>
      qs.get(name) match {
        case None     => Some(factory.newBuilder.result())
        case Some(vs) =>
          // ''traverse'' the list of decoded values
          vs.foldLeft[Option[mutable.Builder[A, CC[A]]]](
              Some(factory.newBuilder)) {
              case (None, _)    => None
              case (Some(b), v) =>
                // Pretend that this was the query string and delegate to the `A` query string param
                param(name, Map(name -> (v :: Nil))).map(b += _)
            }
            .map(_.result())
    }

  implicit def queryStringParamPartialInvFunctor
    : PartialInvariantFunctor[QueryStringParam] =
    new PartialInvariantFunctor[QueryStringParam] {
      override def xmapPartial[A, B](fa: (String, Params) => Option[A],
                                     f: A => Option[B],
                                     g: B => A): (String, Params) => Option[B] =
        (str, params) =>
          for {
            a <- fa(str, params)
            b <- f(a)
          } yield b
    }

  implicit def stringQueryString: QueryStringParam[String] =
    (name, params) => params.get(name).flatMap(_.headOption)

  implicit def segmentPartialInvFunctor: PartialInvariantFunctor[Segment] =
    new PartialInvariantFunctor[Segment] {
      override def xmapPartial[A, B](fa: String => Option[A],
                                     f: A => Option[B],
                                     g: B => A): String => Option[B] =
        s =>
          for {
            a <- fa(s)
            b <- f(a)
          } yield b
    }

  implicit def pathPartialInvariantFunctor: PartialInvariantFunctor[Path] = ???

  def staticPathSegment(segment: String): Path[Unit] = new Path[Unit] {
    def decode(paths: List[String]): Option[(Unit, List[String])] =
      paths match {
        case head :: tail if head == segment => Some(((), tail))
        case _                               => None
      }
  }

  def segment[A](name: String = "", docs: Documentation = None)(
      implicit A: Segment[A]): Path[A] = new Path[A] {
    def decode(segments: List[String]): Option[(A, List[String])] = {
      def uncons[B](bs: List[B]): Option[(B, List[B])] =
        bs match {
          case head :: tail => Some((head, tail))
          case Nil          => None
        }

      uncons(segments).flatMap {
        case (s, ss) =>
          A(s) match {
            case None    => None
            case Some(a) => Option((a, ss))
          }
      }
    }
  }

  implicit def stringSegment: Segment[String] = Some(_)

  def remainingSegments(name: String = "",
                        docs: Documentation = None): Path[String] =
    new Path[String] {
      def decode(segments: List[String]): Option[(String, List[String])] =
        if (segments.isEmpty) None
        else Some((segments.map(URLEncoder.encode(_, utf8Name)).mkString("/"), Nil))
    }

  def chainPaths[A, B](first: Path[A], second: Path[B])(
      implicit tupler: Tupler[A, B]): Path[tupler.Out] = new Path[tupler.Out] {
    override def decode(
        paths: List[String]): Option[(tupler.Out, List[String])] =
      for {
        (firsValue, firstRemainingPaths) <- first.decode(paths)
        (secondValue, secondRemainingPaths) <- second.decode(
          firstRemainingPaths)
      } yield (tupler(firsValue, secondValue), secondRemainingPaths)
  }

  implicit def urlPartialInvFunctor: PartialInvariantFunctor[Url] =
    new PartialInvariantFunctor[Url] {
      override def xmapPartial[A, B](fa: Url[A],
                                     f: A => Option[B],
                                     g: B => A): Url[B] = new Url[B] {
        override def decodeUrl(uri: Uri): Option[B] =
          for {
            a <- fa.decodeUrl(uri)
            b <- f(a)
          } yield b
      }
    }

  /** Builds an URL from the given path and query string */
  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(
      implicit tupler: Tupler[A, B]): Url[tupler.Out] = new Url[tupler.Out] {
    override def decodeUrl(uri: Uri): Option[tupler.Out] = {
      for {
        a <- pathExtractor(path, uri)
        b <- qs(uri.multiParams)
      } yield tupler(a, b)
    }
  }

  private def pathExtractor[A](path: Path[A], uri: http4s.Uri): Option[A] = {
    val segments =
      uri.path
        .split("/")
      .map(URLDecoder.decode(_, utf8Name))
        .to[List]

    path.decode(if (segments.isEmpty) List("") else segments).map(_._1)
  }
}
