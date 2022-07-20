package endpoints4s.algebra.client

import endpoints4s.algebra

trait TimeoutTestSuite[T <: algebra.client.ClientEndpointsTestApi]
  extends ClientTestBase[T] {

  val client: T

  "Timeout" in {
     for {
        _ <- call(client.slowResponseEndpoint, ()).failed.map(_ shouldBe a[scala.concurrent.TimeoutException])
     } yield succeed
  }
}
