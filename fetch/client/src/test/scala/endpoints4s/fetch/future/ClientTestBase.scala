package endpoints4s.fetch.future

import endpoints4s.algebra
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.Future

trait ClientTestBase[T <: algebra.Endpoints] extends AsyncWordSpec with Matchers {

  val client: T

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp]
}
