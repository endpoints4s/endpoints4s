package endpoints.algebra.utils
import endpoints.algebra.User

trait CrudEndpointsTestApi extends CrudEndpoints {

  implicit def userRequest: JsonRequest[User]
  implicit def userResponse: JsonResponse[User]
  implicit def userResponseSeq: JsonResponse[Seq[User]]

  def crud(basePath: String): Crud[User, String, User] =
    restfulCrud[User, String, User](path / basePath)
}
