package endpoints4s.algebra.client

import endpoints4s.algebra.{BasicAuthenticationTestApi, BasicAuthentication}

trait BasicAuthTestSuite[T <: BasicAuthenticationTestApi] extends ClientTestBase[T] {

  def basicAuthSuite() = {

    "Client interpreter" should {

      "authenticate with given credentials" in {
        val credentials = BasicAuthentication.Credentials("user1", "pass2")
        val response = "wiremockeResponse"

        call(client.successProtectedEndpoint, credentials)
          .map(_ shouldEqual Some(response))
      }

      "return None if authentication failed" in {
        val credentials = BasicAuthentication.Credentials("user1", "pass2")

        call(client.failureProtectedEndpoint, credentials)
          .map(_ shouldEqual None)
      }
    }
  }
}
