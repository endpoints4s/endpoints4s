package endpoints4s.openapi

import endpoints4s.{algebra, openapi}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SumTypedRequests extends AnyWordSpec with Matchers {

  "Request bondy content" should {

    "Include all supported content-types" in new Fixtures {
      checkRequestContentTypes(sumTypedEndpoint)(
        Set("text/plain", "application/json")
      )
      checkRequestContentTypes(onlyTextEndpoint)(Set("text/plain"))
      checkRequestContentTypes(onlyJsonEndpoint)(Set("application/json"))
    }
  }

  trait FixtureAlg
      extends algebra.Endpoints
      with algebra.JsonEntitiesFromSchemas
      with algebra.JsonSchemasFixtures {

    import User._ // Extra help for Scala 2.12 to find User json schema

    def sumTypedEndpoint =
      endpoint[Either[User, String], Unit](
        post(path / "user-or-name", jsonRequest[User].orElse(textRequest)),
        ok(emptyResponse)
      )

    def onlyTextEndpoint =
      endpoint[String, Unit](
        post(path / "name", textRequest),
        ok(emptyResponse)
      )

    def onlyJsonEndpoint =
      endpoint[User, Unit](
        post(path / "user", jsonRequest[User]),
        ok(emptyResponse)
      )
  }

  trait Fixtures extends FixtureAlg with openapi.Endpoints with openapi.JsonEntitiesFromSchemas {

    def checkRequestContentTypes[A](
        endpoint: DocumentedEndpoint
    )(contentTypes: Set[String]) = {
      val foundContentTypes = endpoint.item.operations.values.iterator
        .flatMap(_.requestBody.iterator)
        .flatMap(_.content.keys)
        .toSet
      assert(foundContentTypes == contentTypes)
    }

  }
}
