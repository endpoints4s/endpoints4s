package endpoints.akkahttp.server

import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, HttpChallenges, `WWW-Authenticate`}
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, StatusCodes => AkkaStatusCodes}
import akka.http.scaladsl.server.{Directive, Directive1, Directives}
import endpoints.{Tupler, algebra}
import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation

/**
  * @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication with Endpoints {

  private[endpoints] def authenticatedRequest[U, E, H, UE, HCred, Out](
    method: Method,
    url: Url[U],
    entity: RequestEntity[E],
    headers: RequestHeaders[H],
    requestDocs: Documentation
  )(implicit
    tuplerUE: Tupler.Aux[U, E, UE],
    tuplerHCred: Tupler.Aux[H, Credentials, HCred],
    tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Request[Out] = {
    val extractCredentials: HttpHeader => Option[Credentials] = {
      case Authorization(BasicHttpCredentials(username, password)) => Some(Credentials(username, password))
      case _ => None
    }

    val authHeader: Directive1[Credentials] =
      Directives.optionalHeaderValue(extractCredentials).flatMap {
        case Some(credentials) => Directives.provide(credentials)
        case None => Directive[Tuple1[Credentials]] { _ => //inner is ignored
          Directives.complete(HttpResponse(
            AkkaStatusCodes.Unauthorized,
            collection.immutable.Seq[HttpHeader](`WWW-Authenticate`(HttpChallenges.basic("Realm")))
          ))
        }
      }
    joinDirectives(
      joinDirectives(
        joinDirectives(
          convToDirective1(Directives.method(method)),
          url.directive
        ),
        entity
      ),
      headers ++ authHeader
    )
  }

}
