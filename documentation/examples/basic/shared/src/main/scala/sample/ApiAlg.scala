package sample

import endpoints4s.algebra.BasicAuthentication.Credentials
import endpoints4s.algebra._
import io.circe.generic.JsonCodec

trait ApiAlg extends Endpoints with circe.JsonEntitiesFromCodecs with BasicAuthentication {

  val index: Endpoint[(String, Int, String), User] =
    endpoint(
      get(
        path / "user" / segment[String]() /? (qs[Int]("age") & qs[String](
          "toto"
        ))
      ),
      ok(jsonResponse[User])
    )

  val action =
    endpoint(
      post(path / "action", jsonRequest[ActionParameter]),
      ok(jsonResponse[ActionResult])
    )

  val actionFut: Endpoint[ActionParameter, ActionResult] =
    endpoint(
      post(path / "actionFut", jsonRequest[ActionParameter]),
      ok(jsonResponse[ActionResult])
    )

  val maybe =
    endpoint(get(path / "option"), wheneverFound(ok(emptyResponse)))

  val auth: Endpoint[Credentials, Option[Unit]] =
    authenticatedEndpoint(Get, path / "auth", response = ok(emptyResponse))

}

@JsonCodec
case class User(name: String, age: Int)

@JsonCodec
case class ActionParameter()

@JsonCodec
case class ActionResult(s: String)
