package endpoints.play.server

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets.UTF_8

import endpoints.{Invalid, PartialInvariantFunctor, Tupler, Valid, Validated, algebra}
import endpoints.algebra.Documentation
import play.api.libs.functional.{Applicative, Functor}
import play.api.mvc.{RequestHeader, Result, Results}

import scala.collection.compat._
import scala.collection.mutable
import scala.language.higherKinds

/**
  * [[algebra.Urls]] interpreter that decodes and encodes URLs.
  *
  * @group interpreters
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
      def pure[A](a: => A): RequestExtractor[A] =
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
    def decode(segment: String): Validated[A]
    /** @return URL encoded path segment */
    def encode(a: A): String
  }
  //#segment

  implicit lazy val segmentPartialInvFunctor: PartialInvariantFunctor[Segment] = new PartialInvariantFunctor[Segment] {
    def xmapPartial[A, B](fa: Segment[A], f: A => Validated[B], g: B => A): Segment[B] =
      new Segment[B] {
        def decode(s: String) = fa.decode(s).flatMap(f)
        def encode(b: B) = fa.encode(g(b))
      }
    override def xmap[A, B](fa: Segment[A], f: A => B, g: B => A): Segment[B] =
      new Segment[B] {
        def decode(s: String) = fa.decode(s).map(f)
        def encode(b: B) = fa.encode(g(b))
      }
  }

  implicit def stringSegment: Segment[String] =
    new Segment[String] {
      def decode(segment: String) = Valid(segment)
      def encode(s: String) = URLEncoder.encode(s, utf8Name)
    }

  /**
    * Query string encoding and decoding
    */
  trait QueryString[A] {
    /**
      * @return None in case of decoding failure
      * @param qs Map of identifiers and parameter values (these are already URL decoded)
      */
    def decode(qs: Map[String, Seq[String]]): Validated[A]

    /**
      * @return Map of identifiers and URL encoded parameter values
      */
    def encode(a: A): Map[String, Seq[String]] // FIXME Encode to a String for better performance
  }

  implicit lazy val queryStringPartialInvFunctor: PartialInvariantFunctor[QueryString] = new PartialInvariantFunctor[QueryString] {
    def xmapPartial[A, B](fa: QueryString[A], f: A => Validated[B], g: B => A): QueryString[B] =
      new QueryString[B] {
        def decode(qs: Map[String, Seq[String]]): Validated[B] = fa.decode(qs).flatMap(f)
        def encode(b: B): Map[String, Seq[String]] = fa.encode(g(b))
      }
    override def xmap[A, B](fa: QueryString[A], f: A => B, g: B => A): QueryString[B] =
      new QueryString[B] {
        def decode(qs: Map[String, Seq[String]]): Validated[B] = fa.decode(qs).map(f)
        def encode(b: B): Map[String, Seq[String]] = fa.encode(g(b))
      }
  }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
    new QueryString[tupler.Out] {
      def decode(qs: Map[String, Seq[String]]) =
        first.decode(qs).tuple(second.decode(qs))
      def encode(ab: tupler.Out) = {
        val (a, b) = tupler.unapply(ab)
        first.encode(a) ++ second.encode(b)
      }
    }

  def qs[A](name: String, docs: Documentation)(implicit value: QueryStringParam[A]) =
    new QueryString[A] {
      def decode(qs: Map[String, Seq[String]]) =
        value.decode(name, qs).mapErrors(_.map(error => s"$error for query parameter '$name'"))
      def encode(a: A) = value.encode(name, a)
    }

  trait QueryStringParam[A] {
    /**
      * @return a decoded `A` value, or `None` if it was malformed
      */
    def decode(name: String, qs: Map[String, Seq[String]]): Validated[A]
    def encode(name: String, a: A): Map[String, Seq[String]]
  }

  implicit lazy val queryStringParamPartialInvFunctor: PartialInvariantFunctor[QueryStringParam] = new PartialInvariantFunctor[QueryStringParam] {
    def xmapPartial[A, B](fa: QueryStringParam[A], f: A => Validated[B], g: B => A): QueryStringParam[B] =
      new QueryStringParam[B] {
        def decode(name: String, qs: Map[String, Seq[String]]): Validated[B] = fa.decode(name, qs).flatMap(f)
        def encode(name: String, b: B): Map[String, Seq[String]] = fa.encode(name, g(b))
      }
    override def xmap[A, B](fa: QueryStringParam[A], f: A => B, g: B => A): QueryStringParam[B] =
      new QueryStringParam[B] {
        def decode(name: String, qs: Map[String, Seq[String]]): Validated[B] = fa.decode(name, qs).map(f)
        def encode(name: String, b: B): Map[String, Seq[String]] = fa.encode(name, g(b))
      }
  }

  implicit def optionalQueryStringParam[A](implicit param: QueryStringParam[A]): QueryStringParam[Option[A]] =
    new QueryStringParam[Option[A]] {
      def decode(name: String, qs: Map[String, Seq[String]]): Validated[Option[A]] =
        qs.get(name) match {
          case None    => Valid(None)
          case Some(_) => param.decode(name, qs).map(Some(_))
        }
      def encode(name: String, maybeA: Option[A]): Map[String, Seq[String]] =
        maybeA.fold(Map.empty[String, Seq[String]])(param.encode(name, _))
    }

  implicit def repeatedQueryStringParam[A, CC[X] <: Iterable[X]](implicit param: QueryStringParam[A], factory: Factory[A, CC[A]]): QueryStringParam[CC[A]] =
    new QueryStringParam[CC[A]] {
      def decode(name: String, qs: Map[String, Seq[String]]): Validated[CC[A]] =
        qs.get(name) match {
          case None     => Valid(factory.newBuilder.result())
          case Some(vs) =>
            // ''traverse'' the list of decoded values
            vs.foldLeft[Validated[mutable.Builder[A, CC[A]]]](Valid(factory.newBuilder)) {
              case (inv: Invalid, v) =>
                // Pretend that this was the query string and delegate to the `A` query string param
                param.decode(name, Map(name -> (v :: Nil)))
                  .fold(_ => inv, errors => Invalid(inv.errors ++ errors))
              case (Valid(b), v) =>
                // Pretend that this was the query string and delegate to the `A` query string param
                param.decode(name, Map(name -> (v :: Nil))).map(b += _)
            }.map(_.result())
        }
      def encode(name: String, as: CC[A]): Map[String, Seq[String]] =
        if (as.isEmpty) Map.empty
        else Map(name -> as.map(param.encode(name, _)).iterator.flatMap(_.values).flatten.toSeq)
    }

  implicit lazy val stringQueryString: QueryStringParam[String] =
    new QueryStringParam[String] {
      def decode(name: String, qs: Map[String, Seq[String]]) = {
        val maybeValue = qs.get(name).flatMap(_.headOption)
        Validated.fromOption(maybeValue)("Missing value")
      }
      def encode(name: String, s: String) =
        Map(name -> Seq(URLEncoder.encode(s, utf8Name)))
    }

  trait Path[A] extends Url[A] {
    /**
      * @return None in case the incoming path didn’t match, Some(Left(…))
      *         in case it matched but decoding failed, Some(Right(…))
      *         if it matched and decoding succeeded
      */
    def decode(segments: List[String]): Option[(Validated[A], List[String])]
    def encode(a: A): String

    final def decodeUrl(request: RequestHeader) = pathExtractor(this, request)
    final def encodeUrl(a: A) = encode(a)
  }

  implicit lazy val pathPartialInvariantFunctor: PartialInvariantFunctor[Path] = new PartialInvariantFunctor[Path] {
    def xmapPartial[A, B](fa: Path[A], f: A => Validated[B], g: B => A): Path[B] =
      new Path[B] {
        def decode(segments: List[String]): Option[(Validated[B], List[String])] =
          fa.decode(segments).map { case (validA, rs) => (validA.flatMap(f), rs) }
        def encode(b: B): String = fa.encode(g(b))
      }
    override def xmap[A, B](fa: Path[A], f: A => B, g: B => A): Path[B] =
      new Path[B] {
        def decode(segments: List[String]): Option[(Validated[B], List[String])] =
          fa.decode(segments).map { case (validA, rs) => (validA.map(f), rs) }
        def encode(b: B): String = fa.encode(g(b))
      }
  }

  def staticPathSegment(segment: String): Path[Unit] =
    new Path[Unit] {
      def decode(segments: List[String]): Option[(Validated[Unit], List[String])] =
        segments match {
          case s :: ss if s == segment => Some((Valid(()), ss))
          case _ => None
        }
      def encode(unit: Unit): String = segment
    }

  def segment[A](name: String, docs: Documentation)(implicit A: Segment[A]): Path[A] =
    new Path[A] {
      def decode(segments: List[String]) = {
        segments match {
          case head :: tail =>
            val validatedA =
              A.decode(head)
                .mapErrors(_.map(error => s"$error for segment${ if (name.isEmpty) "" else s" '$name'" }"))
            Some((validatedA, tail))
          case Nil => None
        }
      }
      def encode(a: A) = A.encode(a)
    }

  def remainingSegments(name: String, docs: Documentation): Path[String] =
    new Path[String] {
      def decode(segments: List[String]): Option[(Validated[String], List[String])] =
        if (segments.isEmpty) None
        else Some((Valid(segments.map(URLEncoder.encode(_, utf8Name)).mkString("/")), Nil))
      def encode(s: String): String = s // NO URL-encoding because the segments must already be URL-encoded
    }

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    new Path[tupler.Out] {
      def decode(segments: List[String]) =
        first.decode(segments).flatMap { case (validA, segments2) =>
          second.decode(segments2).map { case (validB, segments3) =>
            (validA.tuple(validB), segments3)
          }
        }
      def encode(ab: tupler.Out) = {
        val (a, b) = tupler.unapply(ab)
        first.encode(a) ++ "/" ++ second.encode(b)
      }
    }

  trait Url[A] {
    /**
      * @return `None` if the request doesn’t match, `Some(Invalid(…))` if
      *          it matched but decoding failed, and `Some(Valid(…))`
      *          if it matched and succeeded
      */
    def decodeUrl(req: RequestHeader): Option[Validated[A]]
    def encodeUrl(a: A): String
  }

  implicit lazy val urlPartialInvFunctor: PartialInvariantFunctor[Url] = new PartialInvariantFunctor[Url] {
    def xmapPartial[A, B](fa: Url[A], f: A => Validated[B], g: B => A): Url[B] = new Url[B] {
      def decodeUrl(req: RequestHeader): Option[Validated[B]] = fa.decodeUrl(req).map(_.flatMap(f))
      def encodeUrl(b: B): String = fa.encodeUrl(g(b))
    }
    override def xmap[A, B](fa: Url[A], f: A => B, g: B => A): Url[B] = new Url[B] {
      def decodeUrl(req: RequestHeader): Option[Validated[B]] = fa.decodeUrl(req).map(_.map(f))
      def encodeUrl(b: B): String = fa.encodeUrl(g(b))
    }
  }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] =
    new Url[tupler.Out] {

      def decodeUrl(req: RequestHeader): Option[Validated[tupler.Out]] =
        pathExtractor(path, req).map(_.tuple(qs.decode(req.queryString)))

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

  private def pathExtractor[A](path: Path[A], request: RequestHeader): Option[Validated[A]] = {
      val segments =
        request.path
          .split("/").toList
          .map(s => URLDecoder.decode(s, utf8Name))
      path.decode(if (segments.isEmpty) List("") else segments).flatMap {
        case (validA, Nil) => Some(validA)
        case (_, _)        => None
      }
    }

  /**
    * This method is called by ''endpoints'' when decoding a request failed.
    *
    * The default implementation is to return a Bad Request (400) response
    * containing the error messages as a JSON array of string values.
    *
    * This method can be overridden to customize the error reporting logic.
    */
  def handleClientErrors(invalid: Invalid): Result =
    Results.BadRequest(Endpoints.invalidJsonEncoder.writes(invalid))

}
