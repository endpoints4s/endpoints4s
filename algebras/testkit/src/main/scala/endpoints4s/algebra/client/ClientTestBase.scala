package endpoints4s.algebra.client

import endpoints4s.algebra
import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.Future

trait ClientTestBase[T <: algebra.Endpoints]
    extends AsyncWordSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfter {

  val stubServerPort = 8080

  val client: T

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp]
}
