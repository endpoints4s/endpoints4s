package endpoints.akkahttp.routing

import java.util.Base64

import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives
import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials

trait BasicAuthentication extends algebra.BasicAuthentication with Endpoints {

  /**
    * Extracts the credentials from the request headers.
    * In case of absence of credentials, returns an `Unauthorized` result.
    */
  private[endpoints] lazy val basicAuthentication: RequestHeaders[Credentials] =
    Directives.headerValue(extractCredentials)


  /**
    * Authorization failures can be signaled by returning `None` in the endpoint implementation.
    * In such a case, a `Forbidden` result is returned.
    */
  private[endpoints] def authenticated[A](response: Response[A]): Response[Option[A]] = {
    case Some(a) => response(a)
    case None => Directives.complete(HttpResponse(StatusCodes.Forbidden))
  }


  private def extractCredentials:  HttpHeader => Option[Credentials] = {
    case Authorization(BasicHttpCredentials(username, password)) => Some(Credentials(username, password))
    case _ => None
  }

}
