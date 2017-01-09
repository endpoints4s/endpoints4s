package sample

import endpoints._
import io.circe.generic.JsonCodec

trait ApiAlg extends EndpointAlg with CirceCodecAlg with AssetAlg
  with OptionalResponseAlg with BasicAuthenticationAlg {

  val index =
    endpoint(request(Get, path / "user" / segment[String] /? (qs[Int]("age") & qs[String]("toto"))), jsonResponse[User])

  val action =
    endpoint(request(Post, path / "action", jsonRequest[ActionParameter]), jsonResponse[ActionResult])

  val actionFut =
    endpoint(request(Post, path / "actionFut", jsonRequest[ActionParameter]), jsonResponse[ActionResult])

  lazy val digests = AssetsDigests.digests

  val assets =
    assetsEndpoint(path / "assets" / assetSegments)

  val maybe =
    endpoint(request(Get, path / "option"), option(emptyResponse))

  val auth =
    authenticatedEndpoint(Get, path / "auth", response = emptyResponse)

}

@JsonCodec
case class User(name: String, age: Int)

@JsonCodec
case class ActionParameter()

@JsonCodec
case class ActionResult(s: String)
