package endpoints.algebra.utils
import endpoints.algebra.{Endpoints, JsonEntities, Responses}

trait CrudEndpoints extends Endpoints with JsonEntities with Responses {

  def restfulCrud[Entity : JsonResponse, Id : Segment, EntityReq : JsonRequest](
    basePath: Path[Unit]
  )(implicit eList: JsonResponse[Seq[Entity]]): Crud[Entity, Id, EntityReq] = {
    Crud(
      getAll = endpoint(
        get[Unit, Unit](basePath),
        jsonResponse[Seq[Entity]]()
      ),
      create = endpoint(
        post[Unit, EntityReq, Unit, EntityReq](basePath, jsonRequest[EntityReq]()),
        jsonResponse[Entity]()
      ),
      getById = endpoint(
        get[Id, Unit](basePath / segment[Id]()),
        option(jsonResponse[Entity]())
      ),
      update = endpoint(
        request[Id, EntityReq, Unit, (Id, EntityReq)](
          Put,
          basePath / segment[Id](),
          jsonRequest[EntityReq]()
        ),
        option(jsonResponse[Entity]())
      ),
      delete = endpoint(
        request[Id, Unit, Unit, Id](
          Delete,
          basePath / segment[Id]()
        ),
        option(emptyResponse())
      )
    )

  }

  case class Crud[Entity, Id, EntityReq](
    getAll: Endpoint[Unit, Seq[Entity]],
    create: Endpoint[EntityReq, Entity],
    getById: Endpoint[Id, Option[Entity]],
    update: Endpoint[(Id, EntityReq), Option[Entity]],
    delete: Endpoint[Id, Option[Unit]]
  )
}
