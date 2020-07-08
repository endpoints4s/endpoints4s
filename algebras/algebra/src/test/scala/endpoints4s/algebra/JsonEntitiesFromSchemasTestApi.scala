package endpoints4s.algebra

trait JsonEntitiesFromSchemasTestApi extends EndpointsTestApi with JsonEntitiesFromSchemas {

  implicit val userJsonSchema: JsonSchema[User] = {
    field[String]("name") zip
      field[Int]("age")
  }.xmap(User.tupled)(user => (user.name, user.age))

  val singleStaticGetSegment = endpoint[Unit, User](
    get(path / "user"),
    ok(jsonResponse[User])
  )

  val updateUser =
    endpoint(
      put(path / "user" / segment[Long]("id"), jsonRequest[User]),
      ok(jsonResponse[User])
    )

}
