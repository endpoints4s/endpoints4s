package cqrs.queries

import endpoints4s.play.server.{BuiltInErrors, JsonEntitiesFromCodecs, MuxEndpoints, PlayComponents}
import play.api.routing.Router

import scala.concurrent.Future

/** Implementation of the queries service.
  */
class Queries(service: QueriesService, val playComponents: PlayComponents)
    extends QueriesEndpoints
    with MuxEndpoints
    with BuiltInErrors
    with JsonEntitiesFromCodecs {

  import playComponents.executionContext

  //#multiplexed-impl
  import endpoints4s.play.server.MuxHandlerAsync

  val routes: Router.Routes = routesFromEndpoints(
    query.implementedByAsync(new MuxHandlerAsync[QueryReq, QueryResp] {
      def apply[R <: QueryResp](
          query: QueryReq { type Response = R }
      ): Future[R] =
        //#multiplexed-impl-essence
        query match {
          case FindById(id, t) => service.findById(id, t).map(MaybeResource)
          case FindAll         => service.findAll().map(ResourceList)
        }
      //#multiplexed-impl-essence
    })
  )
  //#multiplexed-impl

  // These aliases are probably due to a limitation of circe
  implicit private def circeDecoderReq: io.circe.Decoder[QueryReq] =
    QueryReq.queryDecoder
  implicit private def circeEncoderResp: io.circe.Encoder[QueryResp] =
    QueryResp.queryEncoder

}
