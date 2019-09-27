# Application-specific authentication

This page explains how to extend the `Endpoints` algebra with vocabulary specific
to the authentication mechanism used by an application, and how to extend interpreters
to implement this authentication mechanism for the server side and the client side.

We will be using Play framework but the same approach can be used for other
HTTP libraries.

We focus on authentication but the same approach can be used for any other
application-specific aspect of the communication that needs to be consistently
implemented by clients and servers.

## Authentication flow

In this example, the authentication information will be encoded in a JSON Web Token (JWT)
attached to HTTP requests. The client will first login to the server, to get
its JWT, and then will use the JWT issued by the server to access to protected
resources. This can be summarized by the following diagram:

~~~ mermaid
sequenceDiagram
client->>server: GET /login
Note over client,server: Client sends its credentials (e.g. a password or an API key)
alt valid credentials
  server-->>client: Ok: JSON Web Token
  Note over client,server: Server replies with a JSON Web Token attached to its response
else invalid credentials
  server-->>client: Bad Request
end
client->>server: GET /protected-resource
Note over client,server: Client tries to access to a protected resource by using its JWT
alt valid JWT
  server-->>client: Ok
else invalid or missing JWT
  server-->>client: Unauthorized
end
~~~

We want to enrich the *endpoints* algebras with new vocabulary describing the login
endpoint as well as the protected endpoints.

## Login endpoint

Let’s start with the login endpoint. This endpoint takes requests containing
credentials and returns responses containing the issued JWT, or an empty
“Bad Request” response in case the credentials where invalid.

### Algebra

The existing algebras already provides all we need to describe such an endpoint,
except for two things:

- encoding the logged in user information as a JWT in the response,
- signalling a bad request in case the authentication failed.

A JWT contains information about the logged-in user (for instance, his name), and that information
is serialized and is cryptographically signed by the server (that’s why clients can not forge an
arbitrary JWT). In our case, the user information we are interested in is only its name:

~~~ scala src=../../../../../documentation/examples/authentication/src/main/scala/authentication/Authentication.scala#user-info-type
~~~

The type used to model the authentication token will be different on client-side and
server-side. On server-side, we are only interested in the user info and we want to let the algebra
interpreter serialize and sign it. However, on client-side we need to also keep the serialized
form since clients can not compute it. Since we want to represent the same concept with different
concrete types on the server and client sides, we model it in the algebra with an abstract
type member `AuthenticationToken`.

In the end, we need to add the following members to our algebra:

~~~ scala src=../../../../../documentation/examples/authentication/src/main/scala/authentication/Authentication.scala#enriched-algebra
~~~

We define our algebra in a trait named `Authentication`, which extends the main
algebra, `algebra.Endpoints`.

Given this new algebra, we can now describe the login endpoint as follows:

~~~ scala src=../../../../../documentation/examples/authentication/src/main/scala/authentication/Usage.scala#login-endpoint
~~~

The `login` endpoint is defined in an `AuthenticationTrait`, which uses (by inheritance)
the main algebra, `algebra.Endpoints`, and the `Authentication` algebra.

The endpoint takes request using the `GET` method, the `/login` URL and a query string
parameter `apiKey` containing the credentials. The returned response is either a
“Bad Request”, or a “Ok” with the issued authentication token.

### Server interpreter

The server interpreter fixes the `AuthenticationToken` type member to `UserInfo`
and implements the `authenticationToken` and `wheneverValid` methods:

~~~ scala src=../../../../../documentation/examples/authentication/src/main/scala/authentication/Authentication.scala#server-interpreter
~~~

The `ServerAuthentication` trait extends the `Authentication` algebra as well
as a server `Endpoints` interpreter based on Play framework.

The `authenticationToken` operation is straightforwardly implemented by
building an `Ok` response and adding it a JWT session containing a `user`
property with the contents of our `UserInfo`. The management of the JWT
session is delegated to the [pauldijou/jwt-scala](https://github.com/pauldijou/jwt-scala)
library, which attaches the issued JWT to the `Authorization` header
of the response.

The `wheneverValid` operation checks whether the response value is defined
or not. In case it is empty, it returns a `BadRequest` response, otherwise
it calls the underlying response.

With this interpreter, the implementation of the login endpoint looks like
the following:

~~~ scala src=../../../../../documentation/examples/authentication/src/main/scala/authentication/Usage.scala#login-implementation
~~~

Our `Server` class extends the traits that defines the `login` endpoint,
namely the `AuthenticationEndpoints`, and mixes the Play-based server
interpreter as well as our `ServerAuthentication` interpreter.

In this simplified example, we only have one valid API key, `"foobar"`, belonging
to Alice. The `login` endpoint is implemented by a function that checks
whether the supplied `apiKey` is equal to `"foobar"`, in which case we return
a `UserInfo` object wrapped in a `Some`. Otherwise we return `None` to signal
that the API key is invalid.

### Mid-way summary

What have we learnt so far?

We are only halfway trough this document but the first sections already
showed the key aspects of enriching the *endpoints* library for
application-specific needs:

1. We have **enriched** the existing algebras with another algebra,
   by defining a trait extending the existing algebras;
2. We have introduced new **concepts** as abstract type members (in
   our case, `AuthenticationToken`);
3. We have introduced new **operations** defining how to
   build or combine concepts together;
4. We have **used** our algebra to define descriptions of endpoints,
   by defining a trait extending the algebra;
5. We have implemented an **interpreter** for our algebra, by
   defining a trait extending the algebra, mixing an existing
   base interpreter and implementing the remaining abstract members;
6. We have **applied** our interpreter to our descriptions of endpoints,
   by defining a class (or an object) extending the endpoint
   descriptions and mixing the interpreter trait.

These relationships are illustrated by the following diagram:

~~~ mermaid
graph BT

Authentication -- "« enrich »" --> algebra.Endpoints
AuthenticationEndpoints -- " « use » " --> Authentication
AuthenticationEndpoints -- " « use » " --> algebra.Endpoints

server.Endpoints -- "« implement »" --> algebra.Endpoints
ServerAuthentication -- "« implement »" --> Authentication
ServerAuthentication -- "« apply »" --> server.Endpoints

Server -- "« use »" --> AuthenticationEndpoints
Server -- "« apply »" --> ServerAuthentication
Server -- "« apply »" --> server.Endpoints

style algebra.Endpoints fill:#eee
style server.Endpoints fill:#eee
~~~

The traits provided by *endpoints* are shown in gray.

### Client interpreter

The implementation of the client interpreter repeats the same
recipe: we define a trait `ClientAuthentication`, which extends
`Authentication` and mixes a `client.Endpoints` base interpreter:

~~~ scala src=../../../../../documentation/examples/authentication/src/main/scala/authentication/Authentication.scala#client-interpreter
~~~

The `AuthenticationToken` type is implemented as a class whose
constructor is private. If it was public, clients could build
a fake authentication token which would then fail at runtime
because the server would reject it when seeing that it is not
correctly signed. By making the constructor private, we simply
make it impossible to reach such a runtime error.

The `AuthenticationToken` class contains the serialized token
as well as the decoded `UserInfo`.

The `authenticationToken` operation is implemented as the dual
of the server interpreter: it checks that there is an `Authorization`
response header, and that it contains a valid `UserInfo` object.
In case of failure, this method returns an exception that
will be eventually thrown by the base client interpreter. One could
argue that we should model the fact that decoding the response can
fail by returning an `Option` instead of throwing an exception.
However, the philosophy of *endpoints* is that client and
server interpreters implement a same HTTP protocol, therefore we
expect (and assume) the interpreters to be consistent together.
Thus, we assume that  don’t need to surface that kind of failures
(hence the use of exceptions).

This contrasts with the `wheneverValid` operation, which
models the fact that the API key supplied by the user can be invalid.
In such a case, we really want the failure to surface to the end-user,
hence the usage of `Option`. The implementation checks whether
the status is 400, in which case it returns `None`, otherwise it
returns the underlying response wrapped in a `Some`.

### Putting things together

If we create an instance of our `Client` an run our `Server`, we can test
that the following scenarios work as expected:

~~~ scala src=../../../../../documentation/examples/authentication/src/test/scala/authentication/AuthenticationTest.scala#login-test-client
~~~

These tests check that if we login with an unknown API key we get no authentication
token, but if we login with the `"foobar"` API key then we get some authentication token.

## Protected endpoints

Now that we are able to issue an authentication token, let’s see how we can define
endpoints that require such an authentication token to be present (and valid) in
incoming requests.

Such protected endpoints take requests containing the serialized token in their
`Authorization` HTTP header, and return a 401 (`Unauthorized`) response in case
the token is not found or is invalid.

### Algebra

To define protected endpoints, we need to enrich the `Authentication` algebra
with additional vocabulary. First, we need a way to define that requests that must
contain the authentication token. Second, we need a way to define that responses
might be `Unauthorized`. Last, we need a convenient `Endpoint` constructor that
puts all the pieces together.

~~~ scala src=../../../../../documentation/examples/authentication/src/main/scala/authentication/Authentication.scala#protected-endpoints-algebra
~~~

The `authenticatedRequest` method defines a request expecting an authentication token
to be provided in the `Authorization` header. The `wheneverAuthenticated` method transforms
a given `Response[A]`
into another `Response[A]` that can be an `Unauthorized` HTTP response in case the
client was not authenticated. Note that, in contrast with the previously defined
`wheneverValid` method, we return a `Response[A]` rather than a `Response[Option[A]]`.
This is because we assume that requests will be built by using the same algebra,
which will make them correctly authenticated by construction.

The last operation we have introduced is `authenticatedEndpoint`, which takes
a request and a response and wraps the request constituents into the `authenticatedRequest`
constructor, and wraps the response into the `wheneverAuthenticated` combinator.

This `authenticatedEndpoint` operation is final, and it is the only user-facing operation
for defining protected endpoints (the two other operations are private). It guarantees
that the request will always have the authentication token in its headers, and that the
response can always be `Unauthorized`.

> {.note}
> The `authenticatedRequest` operation takes several type parameters.
> In particular, they model the type of the request URL (`U`) and entity
> (`E`). These types must be tracked by the type system so that, eventually,
> an `Endpoint[Req, Resp]` is built, where the `Req` type is a tuple of
> all the information (URL and entity) carried by the request.
> In this example we enrich the request headers with the authentication
> token. However, instead of simply returning nested tuples (e.g.
> `((U, E), AuthenticationToken)`), we rely on implicit `Tupler` instances to
> compute the type of the tuple. `Tupler` instances are defined in a way
> that always flattens nested tuples (e.g. they will return
> `(U, E, AuthenticationToken)`) and removes `Unit` types (e.g. if the URL
> is static—of type `Url[Unit]`—the tuplers return `(E, AuthenticationToken)`).

The `authenticatedEndpoint` operation can be used as follows:

~~~ scala src=../../../../../documentation/examples/authentication/src/main/scala/authentication/Usage.scala#protected-endpoint
~~~

Since the request URL is static and the request has no entity, the information carried
by the request is just the `AuthenticationToken`.

### Server interpreter

Our Play-based server is implemented as follows:

~~~ scala src=../../../../../documentation/examples/authentication/src/main/scala/authentication/Authentication.scala#protected-endpoints-server
~~~

And the protected endpoint can be implemented as follows:

~~~ scala src=../../../../../documentation/examples/authentication/src/main/scala/authentication/Usage.scala#protected-resource-implementation
~~~

### Client interpreter

And our Play-based client is implemented as follows:

~~~ scala src=../../../../../documentation/examples/authentication/src/main/scala/authentication/Authentication.scala#protected-endpoints-client
~~~

### Putting things together

Our `Client` and `Server` instances are now able to have more sophisticated exchanges:

~~~ scala src=../../../../../documentation/examples/authentication/src/test/scala/authentication/AuthenticationTest.scala#protected-endpoint-test
~~~

This test first gets an authentication token by calling the `login` endpoint, and then
accesses the protected endpoint by supplying its token.

## Conclusion

This page shows how to include an application-specific aspect of the communication
protocol at the algebra level, and how to implement interpreters for this extended
algebra.

We only demonstrated how to implement client and server interpreters but the same
approach can be used with documentation interpreters.
