package sample.algebra

trait DocumentedApi extends endpoints.algebra.DocumentedEndpoints {

  val getUser =
    endpoint(
      get(path / "users" / segment[Long]("id")),
      emptyResponse("Details of the user")
    )

}
