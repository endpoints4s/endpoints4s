package endpoints4s.algebra.internal

import endpoints4s.algebra.Requests

private[endpoints4s] trait RequestData extends Requests {

  /**
    * The shared data that is necessary in every request so it can be composed into new Requests. Note that this trait
    * is only used internally within endpoints4s.
    *
    * @tparam In The input type for this request, is divided into sub-parts HeadersData, EntityData and UrlData.
    */
  trait RequestData[In] {

    /** The type parameter of the headers, part of the whole input type [[In]] */
    type HeadersData

    /** The type parameter of the request entity, part of the whole input type [[In]] */
    type EntityData

    /** The type parameter of the headers, part of the whole input type [[In]] */
    type UrlData

    /** The URL component of this request */
    def url: Url[UrlData]

    /** The headers component of this request */
    def headers: RequestHeaders[HeadersData]

    /** The method of this request */
    def method: Method

    /* Extract the UrlData from the complete output type of this request */
    def urlData(a: In): UrlData

    /** The entity of this request */
    def entity: RequestEntity[EntityData]  }

}
