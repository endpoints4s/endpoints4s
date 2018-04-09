package endpoints
package openapi

import endpoints.openapi.model.MediaType

/**
  * Interpreter for [[algebra.Requests]].
  */
trait Requests
  extends algebra.Requests
    with Urls
    with Methods {

  type RequestHeaders[A] = DocumentedHeaders

  /**
    * @param value List of request header names (e.g. “Authorization”)
    */
  case class DocumentedHeaders(value: List[DocumentedHeader])

  case class DocumentedHeader(name: String, description: Option[String], required: Boolean)

  def emptyHeaders = DocumentedHeaders(Nil)

  def header(name: String, description: Option[String]): RequestHeaders[String] =
    DocumentedHeaders(List(DocumentedHeader(name, description, required = true)))

  def optHeader(name: String, description: Option[String]): RequestHeaders[Option[String]] =
    DocumentedHeaders(List(DocumentedHeader(name, description, required = false)))


  type Request[A] = DocumentedRequest

  case class DocumentedRequest(
    method: Method,
    url: DocumentedUrl,
    headers: DocumentedHeaders,
    entity: Option[DocumentedRequestEntity]
  )

  type RequestEntity[A] = Option[DocumentedRequestEntity]

  /**
    * @param documentation Human readable documentation of the request entity
    * @param content       Map that associates each possible content-type (e.g. “text/html”) with a [[MediaType]] description
    */
  case class DocumentedRequestEntity(documentation: Option[String], content: Map[String, MediaType])

  def emptyRequest = None

  def request[A, B, C, AB](
    method: Method,
    url: Url[A],
    entity: RequestEntity[B] = emptyRequest,
    headers: RequestHeaders[C] = emptyHeaders
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
    DocumentedRequest(method, url, headers, entity)

  implicit val reqEntityInvFunctor: endpoints.InvariantFunctor[RequestEntity] = new InvariantFunctor[RequestEntity] {
    def xmap[From, To](x: RequestEntity[From], map: From => To, contramap: To => From): RequestEntity[To] = x
  }
  implicit val reqHeadersInvFunctor: endpoints.InvariantFunctor[RequestHeaders] = new InvariantFunctor[RequestHeaders] {
    def xmap[From, To](x: RequestHeaders[From], map: From => To, contramap: To => From): RequestHeaders[To] = x
  }
  implicit val reqHeadersSemigroupK: endpoints.SemigroupK[RequestHeaders] = new SemigroupK[RequestHeaders] {
    def add[A, B](fa: RequestHeaders[A], fb: RequestHeaders[B])(implicit tupler: Tupler[A, B]): RequestHeaders[tupler.Out] =
      DocumentedHeaders(fa.value ++ fb.value)
  }


}
