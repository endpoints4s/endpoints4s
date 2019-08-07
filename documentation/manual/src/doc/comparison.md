# Comparison with similar tools

## Autowire / Remotely / Lagom / Mu

[Autowire](https://github.com/lihaoyi/autowire), and
[Remotely](http://verizon.github.io/remotely), and
[Mu](https://github.com/higherkindness/mu) are Scala libraries automating
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

Last, Autowire, Remotely, Mu, and Lagom can not generate documentation of the communication protocol.

## Swagger / Thrift / gRPC

Solutions such as Swagger, Thrift, and gRPC generate the client and server code
based on a protocol definition written in a custom language.
We believe that generated code is hard to reason about and to integrate and keep
in sync with code written by developers.
Also, the protocol is defined in a dedicated language (JSON dialect or custom language) which
is not extensible and not as convenient as using a fully-blown programming language like Scala.

You can find a more elaborated article about the limitations of approaches based on
code generation in
[this blog post](http://julien.richard-foy.fr/blog/2016/01/24/my-problem-with-code-generation/).

## Rho / Fintrospect / tapir

[Fintrospect](http://fintrospect.io/),
[Rho](https://github.com/http4s/rho), and
[tapir](https://github.com/softwaremill/tapir) projects are comparable alternatives to *endpoints*.
Their features and usage are similar: users describe their communication protocol in plain
Scala and the library generates client (Fintrospect and tapir only), server and documentation.

A key difference is that in these projects the endpoints description language is defined as a sealed AST,
which is not extensible: users can not extend descriptions with application-specific concerns
and interpreters can not be partial. We can illustrate that point with Web Sockets, a feature that is
not be supported by all clients and servers. For instance, Play-WS does not support Web Sockets. This
means that a Web Socket endpoint description can not be interpreted by a Play-WS based client.
There are two ways to inform the user about such an incompatibility: either by showing a compilation error,
or by throwing a runtime exception. In *endpoints*, interpreters can partially support the description
language, resulting in a compilation error if one tries to apply an interpreter that is not powerful
enough to interpret a given endpoint. By contrast, if the description language is a sealed AST then all
interpreters have to be total, otherwise a `MatchError` will be thrown at runtime.

That being said, a drawback of having an extensible description language is that users have to “build”
their language by combining different modules together (eg, `Endpoints with JsonSchemaEntities`), and
then build matching interpreters. These steps are not needed with projects where the description language
is based on a sealed AST.

## Servant / typedapi / typed-schema

[Servant](https://haskell-servant.github.io/) is a Haskell library that uses generic
programming to
derive client, server and documentation from endpoint descriptions.
[typedapi](https://github.com/pheymann/typedapi) and
[typed-schema](https://github.com/TinkoffCreditSystems/typed-schema)
are similar projects written in Scala. In these projects, both descriptions and
interpreters are extensible. The difference with *endpoints* is that
descriptions are **types**, whereas in *endpoints* they are **values**.

Using types as descriptions has some benefits: they can directly be used to type instances of
data (in contrast, in *endpoints* descriptions of data types have to mirror a
corresponding type definition). On the other hand, we believe that abstracting and combining
types using type-level computations is, in general, less convenient for users.
