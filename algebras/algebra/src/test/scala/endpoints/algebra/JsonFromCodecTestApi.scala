package endpoints.algebra

import endpoints.algebra

trait JsonFromCodecTestApi
    extends algebra.Endpoints
    with algebra.JsonEntitiesFromCodecs {

  implicit def userCodec: JsonCodec[User]
  implicit def addressCodec: JsonCodec[Address]

  val jsonCodecEndpoint = endpoint(
    post(path / "user-json-codec", jsonRequest[User]),
    ok(jsonResponse[Address])
  )

}
