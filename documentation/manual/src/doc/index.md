endpoints
=========

*endpoints* is a Scala library for defining communication protocols over HTTP between
applications.

Noteworthy features:

- endpoints descriptions are **first-class Scala values**, which can be reused,
  combined and abstracted over ;
- a *same* endpoint description can be interpreted in *multiple* ways:
    - a **client** implementation (JVM and/or Scala.js),
    - a **server** implementation,
    - an **OpenAPI** definition file,
    - thus, your client and documentation are always **consistent** with the actual
      implementation of your server,
- **type safety**: you get a compile-time error if you invoke an endpoint but supply incorrect data
  (requests are guaranteed to be well constructed),
- **extensibility**: you can introduce both
  - new *descriptions* that are specific to your application (e.g. the usage
    of a particular HTTP header),
  - new *interpreters* for endpoint descriptions (e.g. generation of a RAML documentation),
- **vanilla Scala**: being written in pure Scala (no macros), the implementation is easy to
  reason about and IDE friendly.

## Getting started

- Have a look at the [quick start](quick-start.md) guide to understand
  in a few minutes what the library does and how to setup a project ;
- Check out the [use cases](use-cases.md) to know where *endpoints* shines ;
- Browse the [API documentation](api:endpoints.algebra.package) or the
  [samples](https://github.com/julienrf/endpoints/tree/master/documentation/examples) ;
- Get in touch in the [gitter room](https://gitter.im/julienrf/endpoints).

## Contributing

See the [Github repository](https://github.com/julienrf/endpoints).