package endpoints.algebra

trait JsonFromCodecTestApi {

  val entities: JsonEntitiesFromCodec

  import entities._
  import endpoints._
  import requests._
  import urls._

  implicit def userCodec: entities.JsonCodec[User]
  implicit def addressCodec: entities.JsonCodec[Address]

  lazy val jsonCodecEndpoint = endpoint(
    post(path / "user-json-codec", jsonRequest[User]()),
    jsonResponse[Address]()
  )

}
