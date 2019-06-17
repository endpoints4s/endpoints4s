package endpoints
package openapi

/**
  * Interpreter for [[endpoints.algebra.StatusCodes]]
  *
  * @group interpreters
  */
trait StatusCodes extends endpoints.algebra.StatusCodes {

  sealed trait StatusCode {
    def value: Int
    override def toString = value.toString
  }

  case object OK extends StatusCode {
    val value: Int = 200
  }

  case object BadRequest extends StatusCode {
    val value: Int = 400
  }

  case object NotFound extends StatusCode {
    val value: Int = 404
  }

  case object Unauthorized extends StatusCode {
    val value: Int = 401
  }
}
