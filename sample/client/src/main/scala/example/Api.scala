package example

import io.circe.Decoder
import julienrf.endpoints.XhrClient

object Api extends ApiAlg with XhrClient {

  implicit def userOutput: Decoder[User] = User.dec

}
