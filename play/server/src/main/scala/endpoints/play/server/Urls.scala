package endpoints.play.server

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets.UTF_8

import endpoints.Tupler
import endpoints.algebra
import endpoints.algebra.Documentation
import play.api.libs.functional.{Applicative, Functor}
import play.api.libs.functional.syntax._
import play.api.mvc.RequestHeader

import scala.language.higherKinds
import scala.util.Try

/**
  * [[algebra.Urls]] interpreter that decodes and encodes URLs.
  */
trait Urls extends algebra.Urls {

  val utf8Name = UTF_8.name()

  /**
    * Convenient type alias modeling the extraction of an `A` information from request headers.
    *
    * This type has an instance of [[Applicative]].
    */
  // No Kleisli in play-functional…
  type RequestExtractor[A] = RequestHeader => Option[A]

  // FIXME Pull this algebra up to EndpointsAlg
  implicit lazy val functorRequestExtractor: Functor[RequestExtractor] =
    new Functor[RequestExtractor] {
      def fmap[A, B](m: RequestExtractor[A], f: A => B): RequestExtractor[B] =
        request => m(request).map(f)
    }

  implicit lazy val applicativeRequestExtractor: Applicative[RequestExtractor] =
    new Applicative[RequestExtractor] {
      def apply[A, B](mf: RequestExtractor[A => B], ma: RequestExtractor[A]): RequestExtractor[B] =
        request => mf(request).flatMap(f => ma(request).map(f))
      def pure[A](a: A): RequestExtractor[A] =
        _ => Some(a)
      def map[A, B](m: RequestExtractor[A], f: A => B): RequestExtractor[B] =
        request => m(request).map(f)
    }

  // Temporary?
  implicit final class ApplicativeMapSyntax[F[_], A](fa: F[A]) {
    @inline def map[B](f: A => B)(implicit applicative: Applicative[F]): F[B] = applicative.map(fa, f)
  }

  //#segment
  /** Defines how to decode and encode path segments */
  trait Segment[A] {
    /** @param segment URL decoded path segment */
    def decode(segment: String): Option[A]
    /** @return URL encoded path segment */
    def encode(a: A): String
  }
  //#segment

  implicit def stringSegment: Segment[String] =
    new Segment[String] {
      def decode(segment: String) = Some(segment)
      def encode(s: String) = URLEncoder.encode(s, utf8Name)
    }

  implicit def intSegment: Segment[Int] =
    new Segment[Int] {
      def decode(segment: String) = Try(segment.toInt).toOption
      def encode(a: Int) = a.toString
    }

  implicit def longSegment: Segment[Long] =
    new Segment[Long] {
      def decode(segment: String) = Try(segment.toLong).toOption
      def encode(a: Long) = a.toString
    }

  /**
    * Query string encoding and decoding
    */
  trait QueryString[A] {
    /**
      * @param qs Map of identifiers and parameter values (these are already URL decoded)
      */
    def decode(qs: Map[String, Seq[String]]): Option[A]

    /**
      * @return Map of identifiers and URL encoded parameter values
      */
    def encode(a: A): Map[String, Seq[String]] // FIXME Encode to a String for better performance
  }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
    new QueryString[tupler.Out] {
      def decode(qs: Map[String, Seq[String]]) =
        for {
          a <- first.decode(qs)
          b <- second.decode(qs)
        } yield tupler(a, b)
      def encode(ab: tupler.Out) = {
        val (a, b) = tupler.unapply(ab)
        first.encode(a) ++ second.encode(b)
      }
    }

  def qs[A](name: String, docs: Documentation)(implicit value: QueryStringParam[A]) =
    new QueryString[A] {
      def decode(qs: Map[String, Seq[String]]) = value.decode(name, qs)
      def encode(a: A) = value.encode(name, a)
    }

  def optQs[A](name: String, docs: Documentation)(implicit value: QueryStringParam[A]) =
    new QueryString[Option[A]] {
      def decode(qs: Map[String, Seq[String]]): Option[Option[A]] =
        Some(value.decode(name, qs))
      def encode(maybeA: Option[A]): Map[String, Seq[String]] =
        maybeA.fold(Map.empty[String, Seq[String]])(value.encode(name, _))
    }

  trait QueryStringParam[A] {
    def decode(name: String, qs: Map[String, Seq[String]]): Option[A]
    def encode(name: String, a: A): Map[String, Seq[String]]
  }

  implicit def stringQueryString: QueryStringParam[String] =
    new QueryStringParam[String] {
      def decode(name: String, qs: Map[String, Seq[String]]) =
        qs.get(name).flatMap(_.headOption)
      def encode(name: String, s: String) =
        Map(name -> Seq(URLEncoder.encode(s, utf8Name)))
    }

  implicit def intQueryString: QueryStringParam[Int] =
    new QueryStringParam[Int] {
      def decode(name: String, qs: Map[String, Seq[String]]) =
        qs.get(name).flatMap(_.headOption).flatMap(s => Try(s.toInt).toOption)
      def encode(name: String, i: Int) =
        Map(name -> Seq(i.toString))
    }

  implicit def longQueryString: QueryStringParam[Long] =
    new QueryStringParam[Long] {
      def decode(name: String, qs: Map[String, Seq[String]]) =
        qs.get(name).flatMap(_.headOption).flatMap(s => Try(s.toLong).toOption)
      def encode(name: String, i: Long) =
        Map(name -> Seq(i.toString))
    }

  trait Path[A] extends Url[A] {
    def decode(segments: List[String]): Option[(A, List[String])]
    def encode(a: A): String

    final val decodeUrl = pathExtractor(this)
    final def encodeUrl(a: A) = encode(a)
  }

  def staticPathSegment(segment: String): Path[Unit] =
    new Path[Unit] {
      def decode(segments: List[String]): Option[(Unit, List[String])] =
        segments match {
          case s :: ss if s == segment => Some(((), ss))
          case _ => None
        }
      def encode(unit: Unit): String = segment
    }

  def segment[A](name: String, docs: Documentation)(implicit A: Segment[A]): Path[A] =
    new Path[A] {
      def decode(segments: List[String]) = {
        def uncons[B](bs: List[B]): Option[(B, List[B])] =
          bs match {
            case head :: tail => Some((head, tail))
            case Nil => None
          }
        for {
          (s, ss) <- uncons(segments)
          a <- A.decode(s)
        } yield (a, ss)
      }
      def encode(a: A) = A.encode(a)
    }

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    new Path[tupler.Out] {
      def decode(segments: List[String]) =
        for {
          (a, segments2) <- first.decode(segments)
          (b, segments3) <- second.decode(segments2)
        } yield (tupler(a, b), segments3)
      def encode(ab: tupler.Out) = {
        val (a, b) = tupler.unapply(ab)
        first.encode(a) ++ "/" ++ second.encode(b)
      }
    }

  trait Url[A] {
    def decodeUrl: RequestExtractor[A]
    def encodeUrl(a: A): String
  }

  implicit lazy val urlInvFunctor: endpoints.InvariantFunctor[Url] = new endpoints.InvariantFunctor[Url] {
    override def xmap[From, To](f: Url[From], map: From => To, contramap: To => From): Url[To] = new Url[To] {
      override def decodeUrl: RequestExtractor[To] = f.decodeUrl.map(map)

      override def encodeUrl(a: To): String = f.encodeUrl(contramap(a))
    }
  }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] =
    new Url[tupler.Out] {

      val decodeUrl: RequestExtractor[tupler.Out] =
        pathExtractor(path)
          .and(queryStringExtractor(qs))
          .apply((a, b) => tupler(a, b))

      def encodeUrl(ab: tupler.Out) = {
        val (a, b) = tupler.unapply(ab)
        val encodedQs =
          qs.encode(b)
            .flatMap { case (n, vs) => vs.map(v => (n, v)) }
            .map { case (n, v) => s"$n=$v" }
            .mkString("&")
        s"${path.encode(a)}?$encodedQs"
      }
    }

  private def pathExtractor[A](path: Path[A]): RequestExtractor[A] =
    request => {
      val segments =
        request.path
          .split("/").to[List]
          .map(s => URLDecoder.decode(s, utf8Name))
      path.decode(if (segments.isEmpty) List("") else segments).collect { case (a, Nil) => a }
    }

  private def queryStringExtractor[A](qs: QueryString[A]): RequestExtractor[A] =
    request => qs.decode(request.queryString)

}
