package sample

import endpoints.xhr._

object Api extends ApiAlg with AssetsAlg with Endpoints with JsonEntitiesFromCodec with Assets
  with thenable.Endpoints with BasicAuthentication

