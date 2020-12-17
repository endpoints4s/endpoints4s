# Multiplexed Endpoints

This algebra provides vocabulary to define endpoints multiplexing several
requests and responses.

@coordinates[algebra]

@scaladoc[API documentation](endpoints4s.algebra.MuxEndpoints)

In general, each possible resource or action supported by a service
is exposed through a specific endpoint, taking a specific request type
and a specific response type.

However, in some cases it is useful to use a same endpoint to manage
several resources or actions. We call them *multiplexed* endpoints.

The algebra enriches the `Endpoints` algebra with the concept of
`MuxEndpoint[Req, Resp, Transport]`, defining a multiplexed endpoint
with a request containing a type `Req`, a response of type `Resp`, and
serializing data to and from the `Transport` type.

For instance, the type `MuxEndpoint[Command, Event, Json]` defines
and endpoint whose requests contain `Command` values, whose responses
contain `Event` values, and which serialize commands and events to `Json`.

Since the type of a response can vary according to the type of specific request,
multiplexed endpoints require that request types extend the `MuxRequest` type:

@@snip [MuxEndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/MuxEndpointsDocs.scala) { #mux-endpoint }

Note that the `Command` request type extends `MuxRequest` and that each
concrete `Command` refines its `Response` type member to refer to a
concrete `Event` type.

Typically, client interpreters fix the `MuxEndpoint[Req, Resp, Transport]`
type to be a function from `Req` to `Future[Req#Response]`, so that
calling `users(CreateUser("Alice"))` (statically) returns a `Future[CreatedUser]`,
and calling `users(DeleteUser(42))` returns a `Future[DeletedUser]`.
