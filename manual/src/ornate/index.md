endpoints
=========

*endpoints* is a Scala library for defining communication protocols over HTTP between
applications.

Noteworthy features:

- endpoints descriptions are **first-class Scala values**, which can be reused,
  combined and abstracted over ;
- **consistent client and server implementations** can be derived from endpoint descriptions ;
- **high extensibility**: you can introduce both
  - new descriptions that are specific to your application (e.g. the usage
    of a particular HTTP header),
  - new interpreters for endpoint descriptions (e.g. generation of a swagger-like documentation).

## Getting started

- Have a look at the [overview](overview.md) to understand in a few minutes what
  the library does and how its usage look like ;
- [Install](installation.md) the library and follow the
  [tutorial](tutorial.md) to progressively learn all the features ;
- Explore the [API documentation](api:endpoints.algebra.package) ;
- Get in touch in the [gitter room](https://gitter.im/julienrf/endpoints).

## Contributing

See the [Github repository](https://github.com/julienrf/endpoints).