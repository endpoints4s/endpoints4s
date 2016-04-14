package example

import julienrf.endpoints.Endpoints
import io.circe.generic.JsonCodec
import io.circe.{Decoder, Encoder}

trait ApiAlg extends Endpoints {

  class Api(implicit
    userResponse: JsonResponse[User],
    actionParameterRequest: JsonRequest[ActionParameter],
    actionResultResponse: JsonResponse[ActionResult]
  ) {

    val index = endpoint(get(path / "user" / dynamic), jsonResponse[User])

    val action = endpoint(post(path / "action", jsonRequest[ActionParameter]), jsonResponse[ActionResult])

  }

  // TODO cacheable assets
  // TODO media assets
}

@JsonCodec
case class User(name: String, age: Int)

@JsonCodec
case class ActionParameter()

@JsonCodec
case class ActionResult(s: String)
