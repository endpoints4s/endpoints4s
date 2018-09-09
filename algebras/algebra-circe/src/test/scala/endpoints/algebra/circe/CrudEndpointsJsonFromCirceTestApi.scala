package endpoints.algebra.circe
import endpoints.algebra.utils.CrudEndpointsTestApi
import endpoints.algebra.User

trait CrudEndpointsJsonFromCirceTestApi
  extends CrudEndpointsTestApi
  with endpoints.algebra.circe.JsonEntitiesFromCodec {

  import io.circe._
  import io.circe.generic.semiauto._

  private implicit lazy val userDecoder: Decoder[User] = deriveDecoder[User]
  private implicit lazy val userEncoder: Encoder[User] = deriveEncoder[User]

  override implicit def userRequest: JsonRequest[User]           = jsonCodec
  override implicit def userResponse: JsonResponse[User]         = jsonCodec
  override implicit def userResponseSeq: JsonResponse[Seq[User]] = jsonCodec

}
