package endpoints.algebra.server

import endpoints.algebra
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.duration._

trait ServerTestBase[T <: algebra.Endpoints] extends WordSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll
  with BeforeAndAfter {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds, 10.millisecond)

  val serverApi: T

  /**
    * @param url An URL definition (e.g., `path / "foo"`)
    * @param urlCandidate An URL candidate (e.g., "/foo", "/bar")
    * @return Whether the URL candidate matched the URL definition, or not, or if
    *         decoding failed.
    */
  def decodeUrl[A](url: serverApi.Url[A])(urlCandidate: String): DecodedUrl[A]

}

/**
  * @tparam A The result of decoding an URL candidate
  */
@SerialVersionUID(1L)
sealed trait DecodedUrl[+A] extends Serializable
object DecodedUrl {
  /** The URL candidate matched the given URL definition, and a `A` value was extracted from it */
  case class  Matched[+A](value: A)         extends DecodedUrl[A]
  /** The URL candidate didn’t match the given URL definition */
  case object NotMatched                    extends DecodedUrl[Nothing]
  /** The URL candidate matched the given URL definition, but the decoding process failed */
  case class Malformed(errors: Seq[String]) extends DecodedUrl[Nothing]
}
