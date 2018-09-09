package endpoints.algebra.playjson

import endpoints.algebra
import endpoints.algebra.User
import endpoints.algebra.utils.CrudEndpointsTestApi
import play.api.libs.json.{Format, Json}

trait CrudJsonFromPlayJsonCodecTestApi extends CrudEndpointsTestApi with algebra.playjson.JsonEntitiesFromCodec {

  private implicit lazy val userFormat: Format[User] = Json.format[User]

  override implicit def userRequest: JsonRequest[User]           = jsonCodec
  override implicit def userResponse: JsonResponse[User]         = jsonCodec
  override implicit def userResponseSeq: JsonResponse[Seq[User]] = jsonCodec
}
