package endpoints4s.akkahttp.server

import akka.http.scaladsl.model.Uri

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import scala.collection.compat._
import akka.http.scaladsl.server._
import endpoints4s.algebra.Documentation
import endpoints4s.{Invalid, PartialInvariantFunctor, Tupler, Valid, Validated, algebra}

import scala.collection.mutable

/** [[algebra.Urls]] interpreter that decodes and encodes URLs.
  *
  * @group interpreters
  */
trait Urls extends algebra.Urls with StatusCodes {
  this: EndpointsWithCustomErrors =>

  trait Path[A] extends Url[A] {
    def validate(segments: List[String]): Option[(Validated[A], List[String])]
    final def validateUrl(
        path: List[String],
        query: Map[String, List[String]]
    ): Option[Validated[A]] =
      validate(path).flatMap {
        case (validA, Nil) => Some(validA)
        case (_, _)        => None
      }
    def path(a: A): Uri.Path
    final def uri(a: A): Uri = Uri.Empty.withPath(path(a))
  }

  implicit lazy val pathPartialInvariantFunctor: PartialInvariantFunctor[Path] =
    new PartialInvariantFunctor[Path] {
      def xmapPartial[A, B](
          fa: Path[A],
          f: A => Validated[B],
          g: B => A
      ): Path[B] =
        new Path[B] {
          def validate(segments: List[String]): Option[(Validated[B], List[String])] =
            fa.validate(segments).map { case (validA, ss) =>
              (validA.flatMap(f), ss)
            }
          def path(b: B): Uri.Path = fa.path(g(b))
        }
      override def xmap[A, B](fa: Path[A], f: A => B, g: B => A): Path[B] =
        new Path[B] {
          def validate(segments: List[String]): Option[(Validated[B], List[String])] =
            fa.validate(segments).map { case (validA, ss) => (validA.map(f), ss) }
          def path(b: B): Uri.Path = fa.path(g(b))
        }
    }

  trait Url[A] { outer =>
    def validateUrl(
        segments: List[String],
        query: Map[String, List[String]]
    ): Option[Validated[A]]

    private[server] final def addQueryString[B](queryString: QueryString[B]): Url[(A, B)] =
      new Url[(A, B)] {
        def validateUrl(
            segments: List[String],
            query: Map[String, List[String]]
        ): Option[Validated[(A, B)]] =
          outer.validateUrl(segments, query).map(_.zip(queryString.validate(query)))
        def uri(ab: (A, B)): Uri = {
          val outerUri = outer.uri(ab._1)
          outerUri.withQuery(Uri.Query(outerUri.query() ++ queryString.encode(ab._2): _*))
        }
      }

    final def directive: Directive1[Validated[A]] = {
      (Directives.extractUri & Directives.path(
        Directives.Segments ~ Directives.Slash.?
      ) & Directives.parameterMultiMap)
        .tflatMap { case (uri, segments, query) =>
          val segmentsLeadingTrailingSlash = "" :: segments ++ {
            if (uri.path.endsWithSlash && uri.path != Uri.Path.SingleSlash)
              List("")
            else Nil
          }
          validateUrl(segmentsLeadingTrailingSlash, query) match {
            case None             => Directives.reject
            case Some(validatedA) => Directives.provide(validatedA)
          }
        }
    }
    def uri(a: A): Uri
  }

  trait QueryString[T] {
    def validate(params: Map[String, List[String]]): Validated[T]
    def encode(t: T): Uri.Query
  }

  implicit lazy val queryStringPartialInvariantFunctor: PartialInvariantFunctor[QueryString] =
    new PartialInvariantFunctor[QueryString] {
      def xmapPartial[A, B](
          fa: QueryString[A],
          f: A => Validated[B],
          g: B => A
      ): QueryString[B] = new QueryString[B] {
        def validate(params: Map[String, List[String]]): Validated[B] =
          fa.validate(params).flatMap(f)
        def encode(b: B): Uri.Query = fa.encode(g(b))
      }
      override def xmap[A, B](
          fa: QueryString[A],
          f: A => B,
          g: B => A
      ): QueryString[B] = new QueryString[B] {
        def validate(params: Map[String, List[String]]): Validated[B] = fa.validate(params).map(f)
        def encode(b: B): Uri.Query = fa.encode(g(b))
      }
    }

  /** Given a parameter name and a query string content, returns a decoded parameter
    * value of type `T`, or `Invalid` if decoding failed
    */
  trait QueryStringParam[T] {
    def decode(name: String, params: Map[String, Seq[String]]): Validated[T]
    def encode(name: String, value: T): Uri.Query
  }

  implicit lazy val queryStringParamPartialInvariantFunctor
      : PartialInvariantFunctor[QueryStringParam] =
    new PartialInvariantFunctor[QueryStringParam] {
      def xmapPartial[A, B](
          fa: QueryStringParam[A],
          f: A => Validated[B],
          g: B => A
      ): QueryStringParam[B] = new QueryStringParam[B] {
        def decode(name: String, params: Map[String, Seq[String]]): Validated[B] =
          fa.decode(name, params).flatMap(f)
        def encode(name: String, b: B): Uri.Query = fa.encode(name, g(b))
      }
      override def xmap[A, B](
          fa: QueryStringParam[A],
          f: A => B,
          g: B => A
      ): QueryStringParam[B] = new QueryStringParam[B] {
        def decode(name: String, params: Map[String, Seq[String]]): Validated[B] =
          fa.decode(name, params).map(f)
        def encode(name: String, b: B): Uri.Query = fa.encode(name, g(b))
      }
    }

  trait Segment[A] {
    def validate(s: String): Validated[A]
    def encode(a: A): Uri.Path.Segment
  }

  implicit lazy val segmentPartialInvariantFunctor: PartialInvariantFunctor[Segment] =
    new PartialInvariantFunctor[Segment] {
      def xmapPartial[A, B](
          fa: Segment[A],
          f: A => Validated[B],
          g: B => A
      ): Segment[B] = new Segment[B] {
        def validate(s: String): Validated[B] = fa.validate(s).flatMap(f)
        def encode(b: B): Uri.Path.Segment = fa.encode(g(b))
      }
      override def xmap[A, B](
          fa: Segment[A],
          f: A => B,
          g: B => A
      ): Segment[B] = new Segment[B] {
        def validate(s: String): Validated[B] = fa.validate(s).map(f)
        def encode(b: B): Uri.Path.Segment = fa.encode(g(b))
      }
    }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit
      tupler: Tupler[A, B]
  ): Url[tupler.Out] =
    new Url[tupler.Out] {
      def validateUrl(
          segments: List[String],
          query: Map[String, List[String]]
      ): Option[Validated[tupler.Out]] =
        path.validate(segments).flatMap {
          case (validA, Nil) => Some(validA.zip(qs.validate(query))(tupler))
          case (_, _)        => None
        }
      def uri(out: tupler.Out): Uri = {
        val (a, b) = tupler.unapply(out)
        path.uri(a).withQuery(qs.encode(b))
      }
    }

  //***************
  // Query strings
  //***************

  implicit lazy val stringQueryString: QueryStringParam[String] =
    new QueryStringParam[String] {
      def decode(name: String, params: Map[String, Seq[String]]): Validated[String] = {
        val maybeValue = params.get(name).flatMap(_.headOption)
        Validated.fromOption(maybeValue)("Missing value")
      }
      def encode(name: String, value: String): Uri.Query = Uri.Query(name -> value)
    }

  def qs[A](name: String, docs: Documentation)(implicit
      param: QueryStringParam[A]
  ): QueryString[A] = new QueryString[A] {
    def validate(params: Map[String, List[String]]): Validated[A] =
      param
        .decode(name, params)
        .mapErrors(
          _.map(error => s"$error for query parameter '$name'")
        )
    def encode(a: A): Uri.Query = param.encode(name, a)
  }

  type WithDefault[A] = A

  override def optQsWithDefault[A](name: String, default: A, docs: Documentation = None)(implicit
      value: QueryStringParam[A]
  ): QueryString[WithDefault[A]] =
    qs(name, docs)(optionalQueryStringParam(value)).xmap(_.getOrElse(default))(Some(_))

  implicit def optionalQueryStringParam[A](implicit
      param: QueryStringParam[A]
  ): QueryStringParam[Option[A]] = new QueryStringParam[Option[A]] {
    def decode(name: String, params: Map[String, Seq[String]]): Validated[Option[A]] =
      params.get(name) match {
        case None    => Valid(None)
        case Some(_) => param.decode(name, params).map(Some(_))
      }
    def encode(name: String, value: Option[A]): Uri.Query =
      value match {
        case Some(a) => param.encode(name, a)
        case None    => Uri.Query.Empty
      }
  }

  implicit def repeatedQueryStringParam[A, CC[X] <: Iterable[X]](implicit
      param: QueryStringParam[A],
      factory: Factory[A, CC[A]]
  ): QueryStringParam[CC[A]] = new QueryStringParam[CC[A]] {
    def decode(name: String, params: Map[String, Seq[String]]): Validated[CC[A]] = {
      params.get(name) match {
        case None => Valid(factory.newBuilder.result())
        case Some(vs) =>
          vs.foldLeft[Validated[mutable.Builder[A, CC[A]]]](
            Valid(factory.newBuilder)
          ) {
            case (inv: Invalid, v) =>
              // Pretend that this was the query string and delegate to the `A` query string param
              param
                .decode(name, Map(name -> (v :: Nil)))
                .fold(_ => inv, errors => Invalid(inv.errors ++ errors))
            case (Valid(b), v) =>
              // Pretend that this was the query string and delegate to the `A` query string param
              param.decode(name, Map(name -> (v :: Nil))).map(b += _)
          }.map(_.result())
      }
    }
    def encode(name: String, as: CC[A]): Uri.Query =
      Uri.Query(as.flatMap(a => param.encode(name, a)).toSeq: _*)
  }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit
      tupler: Tupler[A, B]
  ): QueryString[tupler.Out] = new QueryString[tupler.Out] {
    def validate(params: Map[String, List[String]]): Validated[tupler.Out] =
      first.validate(params).zip(second.validate(params))
    def encode(out: tupler.Out): Uri.Query = {
      val (a, b) = tupler.unapply(out)
      Uri.Query(first.encode(a) ++ second.encode(b): _*)
    }
  }

  implicit lazy val urlPartialInvariantFunctor: PartialInvariantFunctor[Url] =
    new PartialInvariantFunctor[Url] {
      def xmapPartial[A, B](
          fa: Url[A],
          f: A => Validated[B],
          g: B => A
      ): Url[B] = new Url[B] {
        def validateUrl(
            segments: List[String],
            query: Map[String, List[String]]
        ): Option[Validated[B]] =
          fa.validateUrl(segments, query).map(_.flatMap(f))
        def uri(a: B): Uri = fa.uri(g(a))
      }

      override def xmap[A, B](fa: Url[A], f: A => B, g: B => A): Url[B] = new Url[B] {
        def validateUrl(
            segments: List[String],
            query: Map[String, List[String]]
        ): Option[Validated[B]] =
          fa.validateUrl(segments, query).map(_.map(f))
        def uri(a: B): Uri = fa.uri(g(a))
      }
    }

  // ********
  // Paths
  // ********

  implicit def stringSegment: Segment[String] = new Segment[String] {
    def validate(s: String): Validated[String] = Valid(s)
    def encode(s: String): Uri.Path.Segment = Uri.Path.Segment(s, Uri.Path.Empty)
  }

  def segment[A](name: String, docs: Documentation)(implicit
      s: Segment[A]
  ): Path[A] = new Path[A] {
    def validate(segments: List[String]): Option[(Validated[A], List[String])] = segments match {
      case head :: tail =>
        val validatedA =
          s.validate(head)
            .mapErrors(
              _.map(error => s"$error for segment${if (name.isEmpty) "" else s" '$name'"}")
            )
        Some((validatedA, tail))
      case Nil => None
    }
    def path(a: A): Uri.Path = s.encode(a)
  }

  def remainingSegments(name: String, docs: Documentation): Path[String] = new Path[String] {
    def validate(segments: List[String]): Option[(Validated[String], List[String])] = {
      if (segments.isEmpty) None
      else
        Some(
          (
            Valid(
              segments.map(URLEncoder.encode(_, UTF_8.name())).mkString("/")
            ),
            Nil
          )
        )
    }
    def path(segments: String): Uri.Path =
      if (segments.isEmpty) Uri.Path.Empty
      else Uri.Path(segments)
  }

  def staticPathSegment(segment: String): Path[Unit] = new Path[Unit] {
    def validate(segments: List[String]): Option[(Validated[Unit], List[String])] = {
      segments match {
        case `segment` :: tail => Some((Valid(()), tail))
        case _                 => None
      }
    }
    def path(a: Unit): Uri.Path =
      if (segment.isEmpty) Uri.Path.Empty
      else Uri.Path.Segment(segment, Uri.Path.Empty)
  }

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit
      tupler: Tupler[A, B]
  ): Path[tupler.Out] = new Path[tupler.Out] {
    def validate(p1: List[String]): Option[(Validated[tupler.Out], List[String])] =
      first.validate(p1).flatMap { case (validA, p2) =>
        second.validate(p2).map { case (validB, p3) =>
          (validA.zip(validB)(tupler), p3)
        }
      }
    def path(out: tupler.Out): Uri.Path = {
      val (a, b) = tupler.unapply(out)
      first.path(a) ++ Uri.Path.SingleSlash ++ second.path(b)
    }
  }

  /** Simpler alternative to `Directive.&()` method
    */
  protected def joinDirectives[T1, T2](
      dir1: Directive1[T1],
      dir2: Directive1[T2]
  )(implicit tupler: Tupler[T1, T2]): Directive1[tupler.Out] = {
    Directive[Tuple1[tupler.Out]] { inner =>
      dir1.tapply { case Tuple1(prefix) =>
        dir2.tapply { case Tuple1(suffix) =>
          inner(Tuple1(tupler(prefix, suffix)))
        }
      }
    }
  }

  protected def convToDirective1(directive: Directive0): Directive1[Unit] = {
    directive.tmap(_ => Tuple1(()))
  }

  /** This method is called by endpoints4s when decoding a request failed.
    *
    * The provided implementation calls `clientErrorsResponse` to complete
    * with a response containing the errors.
    *
    * This method can be overridden to customize the error reporting logic.
    */
  def handleClientErrors(invalid: Invalid): StandardRoute =
    StandardRoute(clientErrorsResponse(invalidToClientErrors(invalid)))

}
