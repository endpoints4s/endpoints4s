# Comparison with similar tools

In this page, we compare endpoints4s with alternative tools that solve the same problem.
We highlights their differences and explain the motivation behind our design decisions.

## Autowire / Remotely / Lagom / Mu

[Autowire](https://github.com/lihaoyi/autowire), and
[Remotely](http://verizon.github.io/remotely), and
[Mu](https://github.com/higherkindness/mu) are Scala libraries automating
remote procedure calls between a server and a client.
[Lagom](https://www.lagomframework.com/) is
a framework for implementing microservices.

The main difference with endpoints4s is that these tools are based on
macros synthesizing the client according to the interface (defined as a Scala trait)
of the server. By contrast, endpoints4s uses no macros.

We chose not to rely on macros because we find that they make it harder to reason about the code
(since they synthesize code that is not seen by the developer), they may not be well supported
by IDEs, and they seem to require a significant effort to support all edge cases (several issues
have been reported about macro expansion: [https://goo.gl/Spco7u](https://goo.gl/Spco7u),
[https://goo.gl/F2E5Ev](https://goo.gl/F2E5Ev) and [https://goo.gl/LCmVr8](https://goo.gl/LCmVr8)).

A more fundamental difference is that in Autowire and Remotely, the underlying HTTP communication
is viewed as an implementation detail, and all remote calls are multiplexed through
a single HTTP endpoint. In contrast, the goal of endpoints4s is to embrace the features
of the HTTP protocol (content negotiation, authorization, semantic verbs and status codes,
etc.), so, in general, one HTTP endpoint is used for one remote call (though the library also
supports multiplexing in case users don’t care about the underlying HTTP protocol).

Last but not least, Autowire, Remotely, Mu, and Lagom can not generate documentation of the
communication protocol.

## Swagger / Thrift / gRPC

Solutions such as Swagger, Thrift, and gRPC generate the client and server code
based on a protocol description written in a custom language, whereas in endpoints4s
descriptions are written in plain Scala and producing a client or a server doesn’t
require generating code.

These custom languages have the benefit of being very clear about their domain, but
they generally lack of means of abstraction (no way to factor out similar parts
of endpoint descriptions) or means of computing (no expression evaluation, no control
structures, etc.). With endpoints4s, developers can easily write a function returning
an endpoint description according to some specific logic and given some parameters.

Tools based on code generators have the benefit that they can be integrated with
virtually any stack (Scala, Rust, etc.). However, we find that they also have some
drawbacks. First, they require users to set up an additional step in their
build, ensuring that the code is generated before compiling the modules that use
it, and that each time the source files are modified the code is re-generated.
Our experience with code generators also showed that sometimes the generated code
does not compile. In such a case, it may be difficult to identify the origin of
the problem because the error is reported on the generated code, not on the code
written by the developer. Furthermore, sometimes the generated code is not convenient
to use as it stands, and developers maintain another layer of abstraction on top of it.
By not relying on code generation, endpoints4s eliminates these potential problems.

You can find a more elaborated article about the limitations of approaches based on
code generation in
[this blog post](http://julien.richard-foy.fr/blog/2016/01/24/my-problem-with-code-generation/).

## Rho / Fintrospect / tapir

[Fintrospect](http://fintrospect.io/),
[Rho](https://github.com/http4s/rho), and
[tapir](https://github.com/softwaremill/tapir) projects are comparable alternatives to endpoints4s.
Their features and usage are similar: users describe their communication protocol in plain
Scala and the library produces clients (Fintrospect and tapir only), servers and documentation.

A key difference is that in these projects the endpoints description language is defined as a sealed AST:
users can not extend descriptions with application-specific concerns
and interpreters can not be partial. We can illustrate that point with Web Sockets, a feature that is
not be supported by all clients and servers. For instance, Play-WS does not support Web Sockets. This
means that a Web Socket endpoint description can not be interpreted by a Play-WS based client.
There are two ways to inform the user about such an incompatibility: either by showing a compilation error,
or by throwing a runtime exception. In endpoints4s, interpreters can partially support the description
language, resulting in a compilation error if one tries to apply an interpreter that is not powerful
enough to interpret a given endpoint description. By contrast, if the description language is a sealed
AST then all interpreters have to be total, otherwise a `MatchError` will be thrown at runtime.

That being said, a drawback of having an extensible description language is that users have to “build”
their language by combining different modules together (eg, `Endpoints with JsonEntitiesFromSchemas`), and
then build matching interpreters. These steps are not needed with projects where the description language
is based on a sealed AST.

## Servant / typedapi / typed-schema

[Servant](https://haskell-servant.github.io/) is a Haskell library that uses generic
programming to
derive client, server and documentation from endpoint descriptions.
[typedapi](https://github.com/pheymann/typedapi) and
[typed-schema](https://github.com/TinkoffCreditSystems/typed-schema)
are similar projects written in Scala. In these projects, both descriptions and
interpreters are extensible. The difference with endpoints4s is that
descriptions are **types**, whereas in endpoints4s they are **values**.

Using types as descriptions has some benefits: they can directly be used to type instances of
data (in contrast, in endpoints4s descriptions of data types have to mirror a
corresponding type definition). On the other hand, we believe that abstracting and combining
types using type-level computations is, in general, less convenient for users.
