package endpoints4s.play.server.circe

/** Convenient trait that groups together [[endpoints4s.play.server.JsonEntitiesFromCodecs]]
  * and [[endpoints4s.algebra.circe.JsonEntitiesFromCodecs]].
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodecs
    extends endpoints4s.play.server.JsonEntitiesFromCodecs
    with endpoints4s.algebra.circe.JsonEntitiesFromCodecs
