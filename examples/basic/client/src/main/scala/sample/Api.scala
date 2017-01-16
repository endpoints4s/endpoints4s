package sample

import endpoints.xhr._

object Api extends ApiAlg with Endpoints with CirceEntities with Assets
  with thenable.Endpoints with OptionalResponses with BasicAuthentication

