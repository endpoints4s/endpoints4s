package example

import io.circe.Decoder
import julienrf.endpoints.XhrClient

object Api extends ApiAlg with XhrClient {

  implicit def userOutput: Decoder[User] = User.dec
  implicit def actionParameterRequest: RequestMarshaller[ActionParameter] = ActionParameter.enc
  implicit def actionResultResponse: ResponseMarshaller[ActionResult] = ActionResult.dec

}
