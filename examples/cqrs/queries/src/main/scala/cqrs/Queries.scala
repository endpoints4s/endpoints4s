package cqrs

import endpoints.algebra.Handler
import endpoints.play.routing.{CirceEntities, Endpoints}
import play.api.mvc.{RequestHeader, Handler => PlayHandler}
import play.api.routing.Router

/**
  * Implementation of the queries service.
  */
object Queries extends QueryEndpoints with Endpoints with CirceEntities {

  val routes: Router.Routes = routesFromEndpoints(
    query.implementedBy(new Handler[QueryReq, QueryResp] {
      def apply[R <: QueryResp](query: QueryReq { type Response = R }): R =
        query match {
          case FindById(id) => MaybeResource(None)
          case Find()       => ResourceList(Nil)
          case FindAll      => ResourceList(Nil)
        }
    })(circeJsonDecoder(QueryReq.queryDecoder), circeJsonEncoder(QueryResp.queryEncoder)) // TODO Let the type inference do the work
  )

}
