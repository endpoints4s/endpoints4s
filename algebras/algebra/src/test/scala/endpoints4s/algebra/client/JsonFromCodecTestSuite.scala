package endpoints4s.algebra.client

import endpoints4s.algebra.{Address, JsonFromCodecTestApi, User}

trait JsonFromCodecTestSuite[T <: JsonFromCodecTestApi] extends ClientTestBase[T] {

  def jsonFromCodecTestSuite() = {

    "Client interpreter" should {

      "encode JSON requests and decode JSON responses" in {

        val user = User("name2", 19)
        val address = Address("avenue1", "NY")

        call(client.jsonCodecEndpoint, user)
          .map(_ shouldEqual address)
      }
    }

  }

}
