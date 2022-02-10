package cqrs.queries

import cats.effect.IO
import endpoints4s.http4s.server

/** Implementation of the queries service.
  */
class Queries(service: QueriesService)
    extends server.Endpoints[IO]
    with server.MuxEndpoints
    with server.JsonEntitiesFromCodecs
    with QueriesEndpoints {

  //#multiplexed-impl
  import endpoints4s.http4s.server.MuxHandlerEffect

  val routes = routesFromEndpoints(
    query.implementedByEffect(new MuxHandlerEffect[Effect, QueryReq, QueryResp] {
      def apply[R <: QueryResp](
          query: QueryReq { type Response = R }
      ): IO[R] =
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
