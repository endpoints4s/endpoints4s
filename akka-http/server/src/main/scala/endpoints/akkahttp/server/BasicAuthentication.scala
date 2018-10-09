package endpoints.akkahttp.server

import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, HttpChallenges}
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directive, Directives}
import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation

/**
  * @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication with Endpoints {

  /**
    * Extracts the credentials from the request headers.
    * In case of absence of credentials rejects request
    */
  private[endpoints] lazy val basicAuthenticationHeader: RequestHeaders[Credentials] =
  Directives.optionalHeaderValue(extractCredentials).flatMap {
    case Some(credentials) => Directives.pass.tmap(_ => credentials)
    case None => Directive[Tuple1[Credentials]] { _ => //inner is ignored
      import akka.http.scaladsl.model.headers
      Directives.complete(HttpResponse(
        StatusCodes.Unauthorized,
        scala.collection.immutable.Seq[HttpHeader](headers.`WWW-Authenticate`(HttpChallenges.basic("Realm")))
      ))
    }
  }


  /**
    * Authorization failures can be signaled by returning `None` in the endpoint implementation.
    * In such a case, a `Forbidden` result is returned.
    */
  private[endpoints] def authenticated[A](response: Response[A], docs: Documentation): Response[Option[A]] = {
    case Some(a) => response(a)
    case None => Directives.complete(HttpResponse(StatusCodes.Forbidden))
  }


  private def extractCredentials: HttpHeader => Option[Credentials] = {
    case Authorization(BasicHttpCredentials(username, password)) => Some(Credentials(username, password))
    case _ => None
  }

}
