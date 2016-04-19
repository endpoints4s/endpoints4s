package example

import endpoints.{CirceCodecs, EndpointsAlg}
import io.circe.generic.JsonCodec

trait ApiAlg extends EndpointsAlg with CirceCodecs {

  val index = endpoint(get(path / "user" / dynamicPathSegment), jsonResponse[User])

  val action = endpoint(post(path / "action", jsonRequest[ActionParameter]), jsonResponse[ActionResult])

  // TODO cacheable assets
  // TODO media assets
}

@JsonCodec
case class User(name: String, age: Int)

@JsonCodec
case class ActionParameter()

@JsonCodec
case class ActionResult(s: String)
