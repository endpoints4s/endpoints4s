package endpoints.play.server.circe

/**
  * Convenient trait that groups together [[endpoints.play.server.JsonEntitiesFromCodec]]
  * and [[endpoints.algebra.circe.JsonEntitiesFromCodec]].
  */
trait JsonEntitiesFromCodec
  extends endpoints.play.server.JsonEntitiesFromCodec
    with endpoints.algebra.circe.JsonEntitiesFromCodec
