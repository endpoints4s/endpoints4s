package endpoints4s.fetch.future

import endpoints4s.algebra

trait BasicAuthTestSuite[T <: BasicAuthenticationTestApi] extends ClientTestBase[T] {

  "Client interpreter" should {

    "authenticate with given credentials" in {
      val credentials = algebra.BasicAuthentication.Credentials("user1", "pass2")
      val response = "wiremockeResponse"

      call(client.successProtectedEndpoint, credentials)
        .map(_ shouldEqual Some(response))
    }

    "return None if authentication failed" in {
      val credentials = algebra.BasicAuthentication.Credentials("user1", "pass2")

      call(client.failureProtectedEndpoint, credentials)
        .map(_ shouldEqual None)
    }
  }
}
