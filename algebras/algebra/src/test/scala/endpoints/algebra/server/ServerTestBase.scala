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

  def serveEndpoint[Resp](endpoint: serverApi.Endpoint[_, Resp], response: Resp): Server

}


