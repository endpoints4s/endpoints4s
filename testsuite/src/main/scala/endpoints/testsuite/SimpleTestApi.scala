package endpoints.testsuite

import endpoints.algebra

/**
  * Created by wpitula on 6/5/17.
  */
trait SimpleTestApi extends algebra.Endpoints {


  val smokeEndpoint = endpoint(
    get(path / "user" / segment[String] / "description" /? (qs[String]("name") & qs[Int]("age"))),
    stringResponse
  )

}
