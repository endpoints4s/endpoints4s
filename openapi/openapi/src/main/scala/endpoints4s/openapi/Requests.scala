package endpoints4s
package openapi

import endpoints4s.algebra.Documentation
import endpoints4s.openapi.model.{MediaType, Schema}

/** Interpreter for [[algebra.Requests]].
  *
  * @group interpreters
  */
trait Requests extends algebra.Requests with Urls with Methods with Headers {

  type RequestHeaders[A] = DocumentedHeaders

  def emptyRequestHeaders = DocumentedHeaders(Nil)

  def requestHeader(name: String, docs: Documentation): RequestHeaders[String] =
    DocumentedHeaders(
      List(DocumentedHeader(name, docs, required = true, Schema.simpleString))
    )

  def optRequestHeader(
      name: String,
      docs: Documentation
  ): RequestHeaders[Option[String]] =
    DocumentedHeaders(
      List(DocumentedHeader(name, docs, required = false, Schema.simpleString))
    )

  type Request[A] = DocumentedRequest

  case class DocumentedRequest(
      method: Method,
      url: DocumentedUrl,
      headers: DocumentedHeaders,
      documentation: Documentation,
      entity: Map[String, MediaType]
  )

  type RequestEntity[A] = Map[String, MediaType]

  lazy val emptyRequest = Map.empty[String, MediaType]

  lazy val textRequest = Map(
    "text/plain" -> MediaType(Some(Schema.simpleString))
  )

  def choiceRequestEntity[A, B](
      requestEntityA: Map[String, MediaType],
      requestEntityB: Map[String, MediaType]
  ): Map[String, MediaType] =
    requestEntityB ++ requestEntityA

  def request[A, B, C, AB, Out](
      method: Method,
      url: Url[A],
      entity: RequestEntity[B] = emptyRequest,
      docs: Documentation = None,
      headers: RequestHeaders[C] = emptyRequestHeaders
  )(implicit
      tuplerAB: Tupler.Aux[A, B, AB],
      tuplerABC: Tupler.Aux[AB, C, Out]
  ): Request[Out] =
    DocumentedRequest(method, url, headers, docs, entity)

  override def addRequestHeaders[A, H](
      request: Request[A],
      headers: RequestHeaders[H]
  )(implicit tupler: Tupler[A, H]): Request[tupler.Out] =
    request.copy(headers = DocumentedHeaders(request.headers.value ++ headers.value))

  override def addRequestQueryString[A, Q](
      request: Request[A],
      qs: QueryString[Q]
  )(implicit tupler: Tupler[A, Q]): Request[tupler.Out] =
    request.copy(url =
      request.url.copy(queryParameters = request.url.queryParameters ++ qs.parameters)
    )

  implicit def requestPartialInvariantFunctor: PartialInvariantFunctor[Request] =
    new PartialInvariantFunctor[Request] {
      def xmapPartial[A, B](
          fa: Request[A],
          f: A => Validated[B],
          g: B => A
      ): Request[B] = fa
    }

  implicit lazy val requestEntityPartialInvariantFunctor
      : endpoints4s.PartialInvariantFunctor[RequestEntity] =
    new PartialInvariantFunctor[RequestEntity] {
      def xmapPartial[From, To](
          x: RequestEntity[From],
          map: From => Validated[To],
          contramap: To => From
      ): RequestEntity[To] = x
    }
  implicit lazy val requestHeadersPartialInvariantFunctor
      : endpoints4s.PartialInvariantFunctor[RequestHeaders] =
    new PartialInvariantFunctor[RequestHeaders] {
      def xmapPartial[From, To](
          x: RequestHeaders[From],
          map: From => Validated[To],
          contramap: To => From
      ): RequestHeaders[To] = x
    }
  implicit lazy val requestHeadersSemigroupal: endpoints4s.Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {
      def product[A, B](fa: RequestHeaders[A], fb: RequestHeaders[B])(implicit
          tupler: Tupler[A, B]
      ): RequestHeaders[tupler.Out] =
        DocumentedHeaders(fa.value ++ fb.value)
    }

}
