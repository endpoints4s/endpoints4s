package example

import julienrf.endpoints.Endpoints
import io.circe.generic.semiauto.deriveFor
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

case class User(name: String, age: Int)

object User {
  implicit val enc: Encoder[User] = deriveFor[User].encoder
  implicit val dec: Decoder[User] = deriveFor[User].decoder
}

case class ActionParameter()

object ActionParameter {
  implicit val enc: Encoder[ActionParameter] = deriveFor[ActionParameter].encoder
  implicit val dec: Decoder[ActionParameter] = deriveFor[ActionParameter].decoder
}

case class ActionResult(s: String)

object ActionResult {
  implicit val enc: Encoder[ActionResult] = deriveFor[ActionResult].encoder
  implicit val dec: Decoder[ActionResult] = deriveFor[ActionResult].decoder
}