package sample

import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra._
import io.circe.generic.JsonCodec

trait ApiAlg extends Endpoints with circe.JsonEntitiesFromCodec with BasicAuthentication {

  val index: Endpoint[(String, Int, String), User] =
    endpoint(get(path / "user" / segment[String]() /? (qs[Int]("age") & qs[String]("toto"))), jsonResponse[User]())

  val action =
    endpoint(post(path / "action", jsonRequest[ActionParameter]()), jsonResponse[ActionResult]())

  val actionFut: Endpoint[ActionParameter, ActionResult] =
    endpoint(post(path / "actionFut", jsonRequest[ActionParameter]()), jsonResponse[ActionResult]())

  val maybe =
    endpoint(get(path / "option"), wheneverFound(emptyResponse()))

  val auth: Endpoint[Credentials, Option[Unit]] =
    authenticatedEndpoint(Get, path / "auth", response = emptyResponse())

}

@JsonCodec
case class User(name: String, age: Int)

@JsonCodec
case class ActionParameter()

@JsonCodec
case class ActionResult(s: String)
