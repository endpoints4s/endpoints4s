package endpoints

import scala.language.higherKinds

trait DocumentedEndpoints extends EndpointType {

  type DocumentedEndpoint[Request, Response] <: Endpoint[Request, Response]

}
