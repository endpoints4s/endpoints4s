package endpoints.algebra

import endpoints.Tupler
import endpoints.algebra.BasicAuthentication.Credentials

trait Builders {
  self: Endpoints =>

  def anEndpoint: EndpointBuilder[Unit, Unit, Unit, Unit] = EndpointBuilder(Get, path, emptyHeaders, emptyRequest, emptyResponse)

  case class EndpointBuilder[U, ReqH, ReqE, RespE]
  (method: Method,
   url: Url[U],
   requestHeaders: RequestHeaders[ReqH],
   requestEntity: RequestEntity[ReqE],
   response: Response[RespE]
  ) {
    def withMethod(m: Method): EndpointBuilder[U, ReqH, ReqE, RespE] =
      this.copy(method = m)

    def withUrl[NewU](u: Url[NewU]): EndpointBuilder[NewU, ReqH, ReqE, RespE] =
      this.copy(url = u)

    def withRequestHeaders[NewReqH](rh: RequestHeaders[NewReqH]): EndpointBuilder[U, NewReqH, ReqE, RespE] =
      this.copy(requestHeaders = rh)

    def addRequestHeaders[NewReqH](rh: RequestHeaders[NewReqH])(implicit tupler: Tupler[ReqH, NewReqH]): EndpointBuilder[U, tupler.Out, ReqE, RespE] = {
      val newHeaders = joinHeaders(this.requestHeaders, rh)
      this.copy(requestHeaders = newHeaders)
    }

    def withRequestEntity[NewReqE](re: RequestEntity[NewReqE]): EndpointBuilder[U, ReqH, NewReqE, RespE] =
      this.copy(requestEntity = re)

    def withResponse[NewRespE](r: Response[NewRespE]): EndpointBuilder[U, ReqH, ReqE, NewRespE] =
      this.copy(response = r)

    def withTextResponse: EndpointBuilder[U, ReqH, ReqE, String] =
      this.copy(response = textResponse)

    def build[UE](implicit tuplerUE: Tupler.Aux[U, ReqE, UE], tuplerUEH: Tupler[UE, ReqH]): Endpoint[tuplerUEH.Out, RespE] =
      endpoint(
        request(this.method, this.url, this.requestEntity, this.requestHeaders),
        this.response
      )
  }


}

trait JsonBuilders extends Builders {
  self: JsonEntities =>

  implicit class EndpointJsonOps[U, ReqH, ReqE, RespE](endp: EndpointBuilder[U, ReqH, ReqE, RespE]) {

    def withJsonRequest[NewReqE: JsonRequest]: EndpointBuilder[U, ReqH, NewReqE, RespE] =
      endp.copy(requestEntity = self.jsonRequest[NewReqE])

    def withJsonResponse[NewRespE: JsonResponse]: EndpointBuilder[U, ReqH, ReqE, NewRespE] =
      endp.copy(response = self.jsonResponse[NewRespE])

  }

}

trait BasicAuthBuilders extends Builders {
  self: BasicAuthentication =>

  implicit class EndpointJsonOps[U, ReqH, ReqE, RespE](endp: EndpointBuilder[U, ReqH, ReqE, RespE]) {

    def withBasicAuth(implicit tupler: Tupler[ReqH, Credentials]): EndpointBuilder[U, tupler.Out, ReqE, Option[RespE]] = {
      val newResponse = authenticated(endp.response)
      endp
        .addRequestHeaders(basicAuthentication)
        .withResponse(newResponse)
    }

  }

}

trait OptionalResponseBuilders extends Builders {
  self: OptionalResponses =>

  implicit class EndpointJsonOps[U, ReqH, ReqE, RespE](endp: EndpointBuilder[U, ReqH, ReqE, RespE]) {

    def withOptionalResponse[NewResp](resp: Response[NewResp]): EndpointBuilder[U, ReqH, ReqE, Option[NewResp]] = {
      val newResponse = option(resp)
      endp
        .withResponse(newResponse)
    }

    def withOptionalResponse: EndpointBuilder[U, ReqH, ReqE, Option[RespE]] = {
      val newResponse = option(endp.response)
      endp
        .withResponse(newResponse)
    }

  }

}