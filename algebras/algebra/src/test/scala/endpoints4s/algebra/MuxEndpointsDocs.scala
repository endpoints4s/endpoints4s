package endpoints4s.algebra

import scala.annotation.nowarn

@nowarn("cat=unchecked")
trait MuxEndpointsDocs extends MuxEndpoints with JsonEntities {

  // Pretend that we use some JSON lib
  type Json
  implicit def jsonJsonRequest: JsonRequest[Json]
  implicit def jsonJsonResponse: JsonResponse[Json]

  //#mux-endpoint
  val users: MuxEndpoint[Command, Event, Json] =
    muxEndpoint[Command, Event, Json](
      post(path / "users", jsonRequest[Json]),
      ok(jsonResponse[Json])
    )

  // Types of commands
  sealed trait Command extends MuxRequest
  final case class CreateUser(name: String) extends Command {
    type Response = UserCreated
  }
  final case class DeleteUser(id: Long) extends Command {
    type Response = UserDeleted
  }

  // Types of responses
  trait Event
  case class UserCreated(id: Long) extends Event
  case class UserDeleted(id: Long) extends Event
  //#mux-endpoint

}
