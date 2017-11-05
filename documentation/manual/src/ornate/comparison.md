# Comparison with similar tools

## Autowire / Remotely / Lagom

[Autowire](https://github.com/lihaoyi/autowire) and
[Remotely](http://verizon.github.io/remotely) are Scala libraries automating
remote proceduce calls between a server and a client.
[Lagom](https://www.lagomframework.com/) is
a framework for implementing microservices. One difference with *endpoints* is
that these tools are based on
macros generating the client according to the interface (defined as a Scala trait)
of the server. These macros make these solutions harder to reason about (since
they synthesize code that is not seen by users) and their implementation might not
support all edge cases (several issues have been reported about macro expansion:
[https://goo.gl/Spco7u](https://goo.gl/Spco7u), [https://goo.gl/F2E5Ev](https://goo.gl/F2E5Ev)
and [https://goo.gl/LCmVr8](https://goo.gl/LCmVr8)).

A more fundamental difference is that in Autowire and Remotely, the underlying HTTP communication
is viewed as an implementation detail, and all remote calls are multiplexed through
a single HTTP endpoint. In contrast, the goal of *endpoints* is to embrace the features
of the HTTP protocol (content negotiation, authorization, semantic verbs and status codes,
etc.), so, in general, one HTTP endpoint is used for one remote call (though the library also
supports multiplexing in case users don’t care about the underlying HTTP protocol).

Last, Autowire, Remotely and Lagom can not generate documentation of the commmunication protocol.

## Swagger / Thrift / Protobuf

Solutions such as Swagger, Thrift and Google Protocol Buffers generate
the client and server code based on a protocol definition.
We believe that generated code is hard to reason about and to integrate and keep
in sync with code written by developers.
Also, the protocol is defined in a dedicated language (JSON dialect or custom language) which
is not extensible and not as convenient as using a fully-blown programming language like Scala.

You can find a more elaborated article about the limitations of approaches based on
code generation in
[this blog post](http://julien.richard-foy.fr/blog/2016/01/24/my-problem-with-code-generation/).

## Rho / Fintrospect

[Fintrospect](http://fintrospect.io/) and
[Rho](https://github.com/http4s/rho) are the libraries closest to *endpoints*.
Their features and usage are similar: users describe their communication protocol in plain
Scala and the library generates client (Fintrospect only), server and documentation.
The key difference is that the communication protocol is described by a sealed AST,
which is not extensible: users can not extend descriptions with application-specific concerns
and interpreters can not be partial.

## Servant

[Servant](https://haskell-servant.github.io/) is a Haskell library that uses generic
programming to
derive client, server and documentation from endpoint descriptions. The descriptions and
interpreters are extensible. The difference with *endpoints* is that in
Servant descriptions are **types**, whereas in *endpoints* they are **values**.

Using types as descriptions has some benefits: they can directly be used to type instances of
data (in contrast, in *endpoints* descriptions of data types have to mirror a
corresponding type definition). On the other hand, we believe that abstracting and combining
types using type-level computations is, in general, less convenient for users.
