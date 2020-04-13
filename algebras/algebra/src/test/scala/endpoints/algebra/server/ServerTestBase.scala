package endpoints.algebra.server

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.`Content-Type`
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.util.ByteString
import endpoints.algebra
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

import scala.concurrent.duration._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.{ExecutionContext, Future}

trait ServerTestBase[T <: algebra.Endpoints]
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll
    with BeforeAndAfter {

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(10.seconds, 10.millisecond)

  val serverApi: T

  /**
    * @param url An URL definition (e.g., `path / "foo"`)
    * @param urlCandidate An URL candidate (e.g., "/foo", "/bar")
    * @return Whether the URL candidate matched the URL definition, or not, or if
    *         decoding failed.
    */
  def decodeUrl[A](url: serverApi.Url[A])(urlCandidate: String): DecodedUrl[A]

  /**
    * @param runTests A function that is called after the server is started and before it is stopped. It takes
    *                 the TCP port number as parameter.
    */
  def serveEndpoint[Resp](
      endpoint: serverApi.Endpoint[_, Resp],
      response: => Resp
  )(runTests: Int => Unit): Unit

  /**
    * @param runTests A function that is called after the server is started and before it is stopped. It takes
    *                 the TCP port number as parameter.
    */
  def serveIdentityEndpoint[Resp](
      endpoint: serverApi.Endpoint[Resp, Resp]
  )(runTests: Int => Unit): Unit

  private[server] implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  val httpClient = Http()

  def sendAndDecodeEntityAsText(
      request: HttpRequest
  ): Future[(HttpResponse, String)] = {
    send(request).map {
      case (response, entity) =>
        (response, decodeEntityAsText(response, entity))
    }
  }

  def send(request: HttpRequest): Future[(HttpResponse, ByteString)] = {
    httpClient.singleRequest(request).flatMap { response =>
      response.entity.toStrict(patienceConfig.timeout).map { entity =>
        (response, entity.data)
      }
    }
  }

  def decodeEntityAsText(response: HttpResponse, entity: ByteString): String = {
    val charset =
      response
        .header[`Content-Type`]
        .flatMap(_.contentType.charsetOption.map(_.nioCharset()))
        .getOrElse(StandardCharsets.UTF_8)
    entity.decodeString(charset)
  }

}

/**
  * @tparam A The result of decoding an URL candidate
  */
sealed trait DecodedUrl[+A] extends Serializable
object DecodedUrl {

  /** The URL candidate matched the given URL definition, and a `A` value was extracted from it */
  case class Matched[+A](value: A) extends DecodedUrl[A]

  /** The URL candidate didn’t match the given URL definition */
  case object NotMatched extends DecodedUrl[Nothing]

  /** The URL candidate matched the given URL definition, but the decoding process failed */
  case class Malformed(errors: Seq[String]) extends DecodedUrl[Nothing]
}
