package sample

import endpoints.algebra._
import io.circe.generic.JsonCodec

trait ApiAlg extends Endpoints with CirceEntities with OptionalResponses with BasicAuthentication {

  val index =
    endpoint(get(path / "user" / segment[String] /? (qs[Int]("age") & qs[String]("toto"))), jsonResponse[User])

  val action =
    endpoint(post(path / "action", jsonRequest[ActionParameter]), jsonResponse[ActionResult])

  val actionFut =
    endpoint(post(path / "actionFut", jsonRequest[ActionParameter]), jsonResponse[ActionResult])

  val maybe =
    endpoint(get(path / "option"), option(emptyResponse))

  val auth =
    authenticatedEndpoint(Get, path / "auth", response = emptyResponse)

}

@JsonCodec
case class User(name: String, age: Int)

@JsonCodec
case class ActionParameter()

@JsonCodec
case class ActionResult(s: String)
