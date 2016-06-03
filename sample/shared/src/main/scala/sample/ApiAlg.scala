package sample

import endpoints.{Assets, CirceCodecs, EndpointsAlg}
import io.circe.generic.JsonCodec

trait ApiAlg extends EndpointsAlg with CirceCodecs with Assets {

  val index = endpoint(get(path / "user" / segment[String] /? (qs[Int]("age") & qs[String]("toto"))), jsonResponse[User])

  val action = endpoint(post(path / "action", jsonRequest[ActionParameter]), jsonResponse[ActionResult])

  lazy val digests = AssetsDigests.digests

  val assets = assetsEndpoint(path / "assets" / assetSegments)

}

@JsonCodec
case class User(name: String, age: Int)

@JsonCodec
case class ActionParameter()

@JsonCodec
case class ActionResult(s: String)
