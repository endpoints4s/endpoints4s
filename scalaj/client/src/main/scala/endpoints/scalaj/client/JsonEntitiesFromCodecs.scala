package endpoints.scalaj.client

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodecs]] that encodes JSON requests
  * and decodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodecs
    extends EndpointsWithCustomErrors
    with endpoints.algebra.JsonEntitiesFromCodecs {

  def jsonRequest[A](implicit codec: JsonCodec[A]): RequestEntity[A] =
    (data, request) => {
      request.header("Content-Type", "application/json")
      request.postData(stringCodec(codec).encode(data))
    }

  def jsonResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[A] =
    resp =>
      stringCodec(codec)
        .decode(resp)
        .fold(Right(_), errors => Left(new Exception(errors.mkString(". "))))

}
