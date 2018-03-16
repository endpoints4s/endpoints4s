package endpoints.testsuite

import endpoints.algebra

trait JsonFromCodecTestApi
  extends algebra.Endpoints
    with algebra.JsonEntitiesFromCodec {

  implicit def userCodec: JsonCodec[User]
  implicit def addressCodec: JsonCodec[Address]

  val jsonCodecEndpoint = endpoint(
    post(path / "user-json-codec", jsonRequest[User]),
    jsonResponse[Address]
  )

}

trait JsonFromCirceCodecTestApi
  extends JsonFromCodecTestApi
    with algebra.circe.JsonEntitiesFromCodec {

  def userCodec = implicitly[JsonCodec[User]]
  def addressCodec = implicitly[JsonCodec[Address]]

}

trait JsonFromPlayJsonCodecTestApi
  extends JsonFromCodecTestApi
    with algebra.playjson.JsonEntitiesFromCodec {

  def userCodec = implicitly[JsonCodec[User]]
  def addressCodec = implicitly[JsonCodec[Address]]

}
