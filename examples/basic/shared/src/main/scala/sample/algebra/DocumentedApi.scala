package sample.algebra

trait DocumentedApi extends endpoints.algebra.DocumentedEndpoints {

  val getUser =
    endpoint(
      get(path / "items" / segment[String]("category") /? optQs[Int]("page")),
      emptyResponse("List all the items of the given category")
    )

}
