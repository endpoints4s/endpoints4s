package endpoints
package openapi

import endpoints.algebra.Documentation
import endpoints.openapi.model.{MediaType, Schema}

/**
  * Interpreter for [[algebra.Requests]].
  *
  * @group interpreters
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

  case class DocumentedHeader(name: String, description: Option[String], required: Boolean, schema: Schema)

  def emptyHeaders = DocumentedHeaders(Nil)

  def header(name: String, docs: Documentation): RequestHeaders[String] =
    DocumentedHeaders(List(DocumentedHeader(name, docs, required = true, Schema.simpleString)))

  def optHeader(name: String, docs: Documentation): RequestHeaders[Option[String]] =
    DocumentedHeaders(List(DocumentedHeader(name, docs, required = false, Schema.simpleString)))


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

  override def textRequest(docs: Documentation): Option[DocumentedRequestEntity] = Some(
    DocumentedRequestEntity(docs, Map("text/plain" -> MediaType(Some(Schema.simpleString))))
  )

  def request[A, B, C, AB, Out](
    method: Method,
    url: Url[A],
    entity: RequestEntity[B] = emptyRequest,
    headers: RequestHeaders[C] = emptyHeaders
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler.Aux[AB, C, Out]): Request[Out] =
    DocumentedRequest(method, url, headers, entity)

  implicit lazy val reqEntityInvFunctor: endpoints.InvariantFunctor[RequestEntity] = new InvariantFunctor[RequestEntity] {
    def xmap[From, To](x: RequestEntity[From], map: From => To, contramap: To => From): RequestEntity[To] = x
  }
  implicit lazy val reqHeadersInvFunctor: endpoints.InvariantFunctor[RequestHeaders] = new InvariantFunctor[RequestHeaders] {
    def xmap[From, To](x: RequestHeaders[From], map: From => To, contramap: To => From): RequestHeaders[To] = x
  }
  implicit lazy val reqHeadersSemigroupal: endpoints.Semigroupal[RequestHeaders] = new Semigroupal[RequestHeaders] {
    def product[A, B](fa: RequestHeaders[A], fb: RequestHeaders[B])(implicit tupler: Tupler[A, B]): RequestHeaders[tupler.Out] =
      DocumentedHeaders(fa.value ++ fb.value)
  }


}
