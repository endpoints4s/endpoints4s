package endpoints4s.algebra.client

import endpoints4s.algebra.TextEntitiesTestApi

trait TextEntitiesTestSuite[T <: TextEntitiesTestApi] extends ClientTestBase[T] {

  def textEntitiesTestSuite() = {

    "TextEntities" should {

      "produce `text/plain` requests with an explicit encoding" in {
        val utf8String = "Oekraïene"

        call(client.textRequestEndpointTest, utf8String)
          .map(_ shouldEqual utf8String)
      }
    }
  }
}
