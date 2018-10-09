package endpoints.scalaj.client

import endpoints.{InvariantFunctor, Tupler, algebra}
import endpoints.algebra.Documentation

import scalaj.http.{Http, HttpRequest}

/**
  * @group interpreters
  */
trait Urls extends algebra.Urls {

  def protocol: String = "http://"

  def address: String

  type QueryString[A] = A => Seq[(String, String)]
  type QueryStringParam[A] = A => String
  type Segment[A] = A => String

  case class Path[A](toStr: A => String) extends Url(toStr.andThen(url => Http(protocol + address + "/" + url)))

  class Url[A](val toReq: A => HttpRequest)

   implicit def stringQueryString: QueryStringParam[String] = identity

   implicit def intQueryString: QueryStringParam[Int] = _.toString

   implicit def longQueryString: QueryStringParam[Long] = _.toString

   def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] = {
    ab => {
      val (a, b) = tupler.unapply(ab)
      first(a) ++ second(b)
    }
  }

   implicit def stringSegment: Segment[String] = identity

   implicit def intSegment: Segment[Int] = _.toString

   implicit def longSegment: Segment[Long] = _.toString


   def qs[A](name: String, docs: Documentation)(implicit value: QueryStringParam[A]): QueryString[A] =
    a => Seq((name, value(a)))

  def optQs[A](name: String, docs: Documentation)(implicit value: QueryStringParam[A]): QueryString[Option[A]] =
    a => a.map(x => Seq((name, value(x)))).getOrElse(Seq())

   def staticPathSegment(segment: String): Path[Unit] = Path(_ => segment)

  def segment[A](name: String, docs: Documentation)(implicit s: Segment[A]): Path[A] = Path(s)

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] = {
    Path(ab => {
      val (a, b) = tupler.unapply(ab)
      val firstStr = first.toStr(a)
      val secondStr = second.toStr(b)
      val separator = if (firstStr.isEmpty || secondStr.isEmpty) "" else "/"
      firstStr + separator + secondStr
    })
  }

  /** Builds an URL from the given path and query string */
   def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] = {
    new Url(ab => {
      val (a, b) = tupler.unapply(ab)
      path.toReq(a)
        .params(qs(b))
    })
  }

  implicit lazy val urlInvFunctor: InvariantFunctor[Url] = new InvariantFunctor[Url] {
    override def xmap[From, To](f: Url[From], map: From => To, contramap: To => From): Url[To] =
      new Url(to => f.toReq(contramap(to)))
  }

}
