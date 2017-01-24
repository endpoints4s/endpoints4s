# Comparison with similar tools

## Autowire

With autowire, all the communications typically go through a single HTTP endpoint and
use a single marshalling format. This has the following consequences :

- You can hardly define RESTful APIs ;
- You can hardly control response caching ;
- You can hardly serve assets.

Another difference is that `endpoints` uses no macros at all.

## Swagger

Swagger provides a set of tools producing documentation resources from an API specification.
The drawback of Swagger is that you have to maintain both the implementation and the
specification and to keep them consistent (even when using the available “integrations”, which
are only able to automate a small part of the specification generation).

With `endpoints` you can write an object algebra producing documentation resources for
endpoint definitions, so that your documentation is derived from the exact same code
that is used to implement the API, hence being always consistent.

## Thrift? Finch? Rho? Lagom? Remotely? Fintrospect?
