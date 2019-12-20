package endpoints.play.server.circe

/**
  * Convenient trait that groups together [[endpoints.play.server.JsonEntitiesFromCodecs]]
  * and [[endpoints.algebra.circe.JsonEntitiesFromCodecs]].
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodecs
  extends endpoints.play.server.JsonEntitiesFromCodecs
    with endpoints.algebra.circe.JsonEntitiesFromCodecs
