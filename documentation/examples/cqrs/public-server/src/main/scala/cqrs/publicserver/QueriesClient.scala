package cqrs.publicserver

import cats.effect.Concurrent
import endpoints4s.http4s.client
import cqrs.queries.QueriesEndpoints
import org.http4s.Uri
import org.http4s.client.{Client => Http4sClient}

class QueriesClient[F[_]: Concurrent](
    authority: Uri.Authority,
    scheme: Uri.Scheme,
    http4sClient: Http4sClient[F]
) extends client.Endpoints[F](authority, scheme, http4sClient)
    with client.JsonEntitiesFromCodecs
    with client.MuxEndpoints
    with QueriesEndpoints
