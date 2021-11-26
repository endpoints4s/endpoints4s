package endpoints4s.algebra.client

import endpoints4s.algebra

trait SumTypedEntitiesTestSuite[
    T <: algebra.SumTypedEntitiesTestApi
] extends ClientTestBase[T] {

  def sumTypedRequestsTestSuite() = {

    "Client interpreter" should {

      "Client interpreter" should {

        "handle the sum-typed request entities" in {
          val user = algebra.User("name2", 19)
          val name = "name3"

          for {
            _ <- call(client.sumTypedEndpoint2, Left(user))
              .map(_.shouldEqual(()))
            _ <- call(client.sumTypedEndpoint2, Right(name))
              .map(_.shouldEqual(()))
          } yield succeed
        }
      }
    }
  }

}
