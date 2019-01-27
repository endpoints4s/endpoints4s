package endpoints.xhr.circe

import endpoints.{algebra, xhr}
import org.scalajs.dom.XMLHttpRequest
import org.scalatest.FreeSpec

import scala.scalajs.js

class JsonSchemaEntitiesTest extends FreeSpec {

  object EndpointsFixtures
    extends JsonSchemaEntities
      with algebra.JsonSchemasTest
      with xhr.future.Endpoints {

    val userRequest = jsonRequest[User]()
    val userResponse = jsonResponse[User]()

  }

  "encoding andThen decoding <=> identity" in {
    val xhr = new XMLHttpRequest()
    xhr.open("GET", "http://foo.com")

    val user = EndpointsFixtures.User("Alice", 42)
    val serialized = EndpointsFixtures.userRequest(user, xhr)
    // Since XHRs are managed by the runtime and are read-only
    // we can not get an XHR response, so we build a fake one here
    val fakeXhr =
      js.Dynamic.literal("responseText" -> serialized).asInstanceOf[XMLHttpRequest]
    val userOrError: Either[Exception, EndpointsFixtures.User] =
      EndpointsFixtures.userResponse(fakeXhr)
    assert(userOrError.right.exists(_ == user))
  }

}
