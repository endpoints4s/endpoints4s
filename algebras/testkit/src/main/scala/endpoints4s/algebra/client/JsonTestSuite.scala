package endpoints4s.algebra.client

import endpoints4s.algebra.{Address, JsonTestApi, User}

trait JsonTestSuite[T <: JsonTestApi] extends ClientTestBase[T] {

  def clientTestSuite() = {

    "Client interpreter" should {

      "return server json response" in {
        val user = User("name2", 19)
        val address = Address("avenue1", "NY")

        call(client.jsonEndpoint, user).map(_ shouldEqual address)
      }
    }

  }

}
