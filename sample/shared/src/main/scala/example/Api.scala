package example

import endpoints.{CirceCodecs, Endpoints}
import io.circe.generic.JsonCodec

trait ApiAlg extends Endpoints with CirceCodecs {

  val index = endpoint(get(path / "user" / dynamic), jsonResponse[User])

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
