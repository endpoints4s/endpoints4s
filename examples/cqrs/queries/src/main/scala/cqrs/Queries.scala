package cqrs

import endpoints.algebra.Handler
import endpoints.play.routing.{CirceEntities, Endpoints}
import play.api.mvc.{RequestHeader, Handler => PlayHandler}

object Queries extends QueryEndpoints with Endpoints with CirceEntities {

  val routes: PartialFunction[RequestHeader, PlayHandler] = routesFromEndpoints(
    query.implementedBy(new Handler[Query, QueryResult] {
      def apply[R <: QueryResult](query: Query { type Response = R }): R =
        query match {
          case FindById(id) => MaybeResource(None)
          case Find()       => ResourceList(Nil)
          case FindAll      => ResourceList(Nil)
        }
    })(circeJsonDecoder(Query.queryDecoder), circeJsonEncoder(QueryResult.queryEncoder)) // TODO Let the type inference do the work
  )

}
