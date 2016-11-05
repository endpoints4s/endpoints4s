package endpoints

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

trait LowLevelEndpointPlayRouting extends LowLevelEndpointAlg with EndpointPlayRouting {

  /** Represents a request entity as a Play `Request[AnyContent]` */
  type RawRequestEntity = play.api.mvc.Request[AnyContent]

  lazy val rawRequestEntity: RequestEntity[RawRequestEntity] =
    BodyParser { requestHeader =>
      val accumulator =
        BodyParsers.parse.anyContent.apply(requestHeader)
      accumulator.map(_.right.map(anyContent => Request(requestHeader, anyContent)))
    }

  /** An HTTP response is a Play [[Result]] */
  type RawResponseEntity = Result

  lazy val rawResponseEntity: Response[RawResponseEntity] = identity

}
