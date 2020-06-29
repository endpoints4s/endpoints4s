package sample

import endpoints4s.xhr._

object Api
    extends ApiAlg
    with AssetsAlg
    with Endpoints
    with JsonEntitiesFromCodecs
    with Assets
    with thenable.Endpoints
    with BasicAuthentication
