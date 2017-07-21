# Comparison with similar tools

## Autowire

Autowire's purpose is to handle client/server remote procedure calls.
The purpose of the endpoints library is to work with HTTP endpoints, which
includes (but is not limited to) client/server remote procedure calls.

If you just want to perform RPC and that the underlying protocol does
not matter, you should probably just pick Autowire.

If you want to perform RPC but at the same time define the structure of the
unerlying HTTP requests and responses, if you want to serve media resources,
or if you experience troubles with Autowire’s macros, you should give a try
to the endpoints library.

## Swagger

Swagger provides a set of tools producing documentation resources from an API specification.
The drawback of Swagger is that you have to maintain both the implementation and the
specification and to keep them consistent (even when using the available “integrations”, which
are only able to automate a small part of the specification generation).

`endpoints` takes an opposite approach: we generate an OpenAPI definition
file from a description of endpoints written in Scala. The benefit is that the
documentation is always consistent with the server and client implementations
(as long as they are all derived from the same description of endpoints).

## Thrift? Finch? Rho? Lagom? Remotely? Fintrospect? Scala-json-rpc?
